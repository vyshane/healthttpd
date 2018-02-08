// Copyright 2018 Vy-Shane Xie Sin Fat

package mu.node.healthttpd

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.{ExecutorService, Executors}
import java.util.{ArrayList, Collections, List}

import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoHTTPD.Response
import mu.node.healthttpd.Healthttpd.HealthChecker
import org.slf4j.LoggerFactory

/*
 * Healthttpd serves health status at /health and readiness status at /readiness
 */
class Healthttpd private(val port: Int, private var isHealthy: HealthChecker = () => true) extends NanoHTTPD(port) {

  private val log = LoggerFactory.getLogger(this.getClass)
  val isReady = new AtomicBoolean(false)

  def startAndIndicateNotReady(): Healthttpd = {
    setAsyncRunner(BoundRunner(Executors.newFixedThreadPool(2)))
    start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
    log.info(s"Serving health status on port $port at /health")
    log.info(s"Serving readiness status on port $port at /readiness")
    this
  }

  def setHealthCheck(isHealthy: () => Boolean): Healthttpd = this.synchronized {
    this.isHealthy = isHealthy
    this
  }

  def indicateReady(): Healthttpd = {
    log.info(s"Serving status Ready")
    isReady.set(true)
    this
  }

  override def serve(session: NanoHTTPD.IHTTPSession): Response = {
    session.getUri match {
      case "/health"    => serveHealth()
      case "/readiness" => serveReadiness()
      case _            => serveNotFound()
    }
  }

  private def serveHealth(): Response = {
    if (isHealthy()) respond(Response.Status.OK, "Healthy")
    else respond(Response.Status.INTERNAL_ERROR, "Unhealthy")
  }

  private def serveReadiness(): Response = {
    if (isReady.get()) respond(Response.Status.OK, "Ready")
    else respond(Response.Status.SERVICE_UNAVAILABLE, "Not Ready")
  }

  private def serveNotFound(): Response = {
    respond(Response.Status.NOT_FOUND, "404 Not Found")
  }

  private def respond(status: Response.Status, message: String): Response = {
    NanoHTTPD.newFixedLengthResponse(status, "text/html; charset=utf-8", message)
  }
}

object Healthttpd {
  type HealthChecker = () => Boolean

  def apply(port: Int) = new Healthttpd(port)
}

/*
 * The default threading strategy for NanoHTTPD launches a new thread every time. We override
 * that here so we can put an upper limit on the number of active threads using a thread pool.
 */
case class BoundRunner(val executorService: ExecutorService) extends NanoHTTPD.AsyncRunner {

  private val running: List[NanoHTTPD#ClientHandler] = Collections.synchronizedList(new ArrayList)

  override def closeAll(): Unit = {
    import scala.collection.JavaConverters._
    // Use a copy of the list for concurrency
    for (clientHandler <- new ArrayList[NanoHTTPD#ClientHandler](running).asScala) {
      clientHandler.close()
    }
  }

  override def closed(clientHandler: NanoHTTPD#ClientHandler): Unit = {
    running.remove(clientHandler)
  }

  override def exec(clientHandler: NanoHTTPD#ClientHandler): Unit = {
    executorService.submit(clientHandler)
    running.add(clientHandler)
  }
}
