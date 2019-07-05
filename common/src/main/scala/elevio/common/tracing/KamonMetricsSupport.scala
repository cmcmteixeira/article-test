package elevio.common.tracing

import cats.effect.IO
import kamon.Kamon
import kamon.trace.Tracer.SpanBuilder
import kamon.trace.{Span, SpanCustomizer}
import cats.implicits._

trait KamonMetricsSupport {

  sealed trait Tag

  //This type of tag will be added to the span but not to the metrics (e.g. it will NOT be available in Zipkin/Jaeger but not as in grafana/influxdb
  case class SpanTag(key: String, value: String) extends Tag
  //Thys type of tag will be addded to the span & the metrics (e.g. it will be available both in Zipking/Jaeger and in grafana/influxdb)
  case class MetricTag(key: String, value: String) extends Tag

  //Used for child operations (i.e. and operation execution within the context of an incoming http request)
  def measured[T](
      operation: String, //name of the operation
      tags: List[Tag] = List.empty, //these are tags that will be available
      onSuccess: T => List[Tag] = (_: T) => List(MetricTag("success", "true")), //when the io is successful , this tag will be added
      onException: Throwable => List[Tag] = (_: Throwable) => List(MetricTag("success", "false")) //when the io fails w/ an exception, this tags will be added
  )(io: IO[T]): IO[T] =
    (for {
      context     <- IO(Kamon.currentContext())
      clientSpan  <- IO(context.get(Span.ContextKey))
      spanBuilder <- IO(spanFor(operation, clientSpan, tags))
      span        <- IO(context.get(SpanCustomizer.ContextKey).customize(spanBuilder).start())
      newCtx      <- IO(context.withKey(Span.ContextKey, span))
      scope       <- IO(Kamon.storeContext(newCtx))
      result      <- io.attempt
      _           <- IO(Kamon.storeContext(context))
      _           <- IO(handleResult(span)(onSuccess, onException)(result))
      _           <- IO.delay(scope.close())
      _           <- IO.delay(span.finish())
    } yield result).rethrow

  private def spanFor(operationName: String, clientSpan: Span, tags: List[Tag]): SpanBuilder = {
    val all = MetricTag("span.kind", "custom") :: MetricTag("component", "app") :: tags
    all.foldLeft(Kamon.buildSpan(operationName).asChildOf(clientSpan))((span, metric) =>
      metric match {
        case SpanTag(key, value)   => span.withTag(key, value)
        case MetricTag(key, value) => span.withMetricTag(key, value)
    })
  }

  //Notice: this function is not referentially transparent due to the span api being stateful
  private def handleResult[T](span: Span)(onSuccess: T => List[Tag], onException: Throwable => List[Tag])(result: Either[Throwable, T]) = {
    val spanWithUserMetrics = result
      .fold(onException, onSuccess)
      .foldLeft(span)((span, metric) =>
        metric match {
          case SpanTag(key, value)   => span.tag(key, value)
          case MetricTag(key, value) => span.tagMetric(key, value)
      })
    result.fold(t => spanWithUserMetrics.addError("application.failure", t), _ => spanWithUserMetrics)
  }

}
