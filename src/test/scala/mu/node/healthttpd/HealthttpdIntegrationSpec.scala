// Copyright 2018 Vy-Shane Xie Sin Fat

package mu.node.healthttpd

import org.jsoup.{Connection, HttpStatusException, Jsoup}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}

class HealthttpdIntegrationSpec extends WordSpec with Matchers with BeforeAndAfterAll {

  val port = 8080
  val healthEndpoint = s"http://127.0.0.1:$port/health"
  val readinessEndpoint = s"http://127.0.0.1:$port/readiness"
  val healthttpd = Healthttpd(port).startAndIndicateNotReady()

  override def afterAll() = {
    healthttpd.stop()
  }

  "The StatusServer" when {
    "it is started" should {
      "serve healthy and not ready statuses" in {
        Jsoup.connect(healthEndpoint).method(Connection.Method.GET).execute().statusCode() shouldEqual 200
        val error = intercept[HttpStatusException] {
          Jsoup.connect(readinessEndpoint).method(Connection.Method.GET).execute()
        }
        error.getStatusCode shouldEqual 503
      }
    }
    "it is asked to indicate ready" should {
      "serve ready status" in {
        healthttpd.indicateReady()
        Jsoup.connect(readinessEndpoint).method(Connection.Method.GET).execute().statusCode() shouldEqual 200
      }
    }
    "when the health checker returns false it" should {
      "serve unhealthy status" in {
        healthttpd.setHealthCheck(() => false)
        val error = intercept[HttpStatusException] {
          Jsoup.connect(healthEndpoint).method(Connection.Method.GET).execute()
        }
        error.getStatusCode shouldEqual 500
      }
    }
    "it is asked for an inexistant endpoint" should {
      "return a 404 Not Found response" in {
        val error = intercept[HttpStatusException] {
          Jsoup.connect(s"${readinessEndpoint}-does-not-exist").method(Connection.Method.GET).execute()
        }
        error.getStatusCode shouldEqual 404
      }
    }
  }
}
