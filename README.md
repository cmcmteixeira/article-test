#### Getting Started
- Install [docker](https://docs.docker.com/install/) and [docker-compose](https://docs.docker.com/compose/install/)
- Add the Elevio Token and service API's to `docker-compose.yml` both for the article and for the updater services (notice that the Bearer part has to be included). 
```yaml
 article:
    ...
    environment:
      ELEVIO_TOKEN: "Bearer a_token"
      ELEVIO_API_KEY: "an_api_key"
  updater:
    ...
    environment:
      ELEVIO_TOKEN: "Bearer a_token"
      ELEVIO_API_KEY: "an_api_key"
```
- Run `docker-compose build` (unfortunately this may take 
a while as the dependencies have to be fetched and both projects 
are build independently; it usually takes less than 10 minutes)
- Run `docker-compose up -d` (due to concurrency issues, it may happen that the services are 
started before rabbit and postgres are ready to accept connections)
- Check if the article service is up by running `curl -v http://127.0.0.1:9000/_meta/health` ; 
if the reponse is 200 then the service should be up, if not; 
running `docker-compose up -d` again should start the service and fix the issue
- Perform the same check for the article updater service by running `curl -v http://127.0.0.1:9001/_meta/health` ; 
If the service is not up, running `docker-compose up -d` should fix it as well
- Go to `localhost:9000` !!!


#### Paginated View

The paginated view offers a way to visualize all the articles in your Elevio account.
On the left, you can see the list of items; clicking on of those items will show you its details on the right.

Typing a **keyword** on the input box and then clicking search,  will filter the paginated list by keyword. Notice that, when filtered
by keyword, this list of results may not be up to date (check Article Updater for details).

![](imgs/ui.png)

#### Article Service
The article service is exposed on port 9000.  

| Endpoint  | Description |
| ------------- | ------------- |
| /articles/:id  | Fetches the most up to date details for article :id. |
| /articles?page=1&keyword=k1&keyword=k2  | Lists articles. Page is mandatory. |
| /internal/articles/:id  | Fetches the stored values for article :id. |
| /internal/articles?page=1&itemsPerPage=10  | Fetches the paginated stored list of values. Both `page` and `itemsPerPage` are mandatory |

 
#### Article Updater
 
In order to avoid going over the api limits of 10 requests per second and support searching by keyword in a paginated manner, 
the Article Service stores a list of articles and their keywords. 

This "cache" of values is bound to go stale if not refreshed periodically so the article updater(a separate service)
continually searches and compares the elevio and article service state. 
When a difference is found, it publishes an event into rabbit which will get picked up/consumed by the article service.
The speed/rate at which this service runs can be configured using the Updater `application.conf`
Note: config changes require a new `docker-compose build` & `docker-compose up` to take effect

```.hocon
internal-walker {
  parallelization: 10
  items-per-page: 2
  rate: 1 second
}

elevio-walker {
  parallelization: 10
  rate: 1 second
}
```

#### Logging
Correlation id's are being printed with the log calls which can be used to group logs generated
within the same call/transaction.
In order to check the logs printed by the service you can run:
`docker-compose logs -f article`
![](imgs/logs.png)

Note: Unfortunately it seems like Kamon does not fully support fs2 which is why the `updater` service
does not generate/log this correlation id.

#### Zipkin 
Zipkin can be viewed on port 9411.
![](imgs/zipkin.png)


#### Grafana
Grafana is running on port 3000.
 ![](imgs/grafana.png)
 
Note: In order to make grafana dashboards available, a volume `docker/data/grafana` is mounted in the 
container.
There are two dashboards , one for the Updater and another for the Actual Service




#### Possible Improvements

- Add throttle mechanism to the Article service; even thought throttling exists on the updater side, if a long batch of article update
events accumulates in rabbit, then it's likely that the Article service will go over the api rate limit. This will result in requing which means the update functionality still works but it may make it impossible for users to intermitently get some errors.
- Fix issue between fs2 / kamon . (Newer version of Kamon is likely to address the issue).
- Improve Keyword filtering mechanism. Currently there's no way of searching for articles that have one of two keywords without making two requests; the current model/implementation combines filters using AND only.
- Add integration tests (currently only Unit tests were added)
- Add Pact contracts between services and/or expose client libraries.
- Improve UI; having to click "Search" is not ideal; make it prettier as well
- Integrate with the ELK stash so that logs are more easily queriable & so that we can plot log information in Grafana
- Allow for multiple retries on service startup (this would not be a problem with tools like Kubernetes or EC2 which can restart the service automatically if the healthcheck endpoints fail but it's not ideal with docker-compose) so that starting the service multiple times is not necessary

#### Tools & Libraries used
- cats
- cats-effect 
- http4s (HTTP server & client)
- circe (JSON encoding/decoding)
- Kamon (Tracing & Metric collection)
- Doobie (Database Read/Write)
- Flyway (Database Migrations)
- Bucky (RabbitMQ Library)
- Docker/Docker-compose (orchestration)
- Grafana (Metrics dashboards)
- InfluxDB (Time series storage)
- RabbitMQ 
- Postgres (Database)
- Zipking (Trace visualization & Storage)

#### 
This project was loosely inspired by [this](https://github.com/cmcmteixeira/foobar-tracing-demo) and [this](https://github.com/porchgeese/service.g8) template was used to create the service (though some alterations had to be made because the template is not 100% correct). 

Note:  (both the porchgeese org and the foobar demo are owned by me)
