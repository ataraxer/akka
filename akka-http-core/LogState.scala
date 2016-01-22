package playground

import akka.stream.stage._


case class LogState[T](tag: String) extends PushStage[T, T] {
  override def onPush(elem: T, ctx: Context[T]) = {
    ctx.push(elem)
  }

  override def onUpstreamFinish(ctx: Context[T]) = {
    println(f"Upstream closed: $tag")
    ctx.finish()
  }

  override def onDownstreamFinish(ctx: Context[T]) = {
    println(f"Downstream closed: $tag")
    ctx.finish()
  }
}

