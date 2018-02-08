# Healthttpd

A tiny Scala library that provides a lightweight health and readiness status server.

Healthttpd serves application status via HTTP/1.1. It provides two endpoints.

### /readiness

Readiness indicates whether the application has finished initialization and is ready to do its job. You can poll this endpoint until you get a HTTP code 200 and then add the application to your load balancer.

| Status | HTTP Code |
| --- | --- | --- |
| Ready | 200 |
| Not Ready | 503 |

### /health

Health indicates whether the application is healthy. For example, can it access all the resources that it depends on to do its job? If you get a HTTP code 500 back you can remove the application from your load balancer. Add it back in when the application is healthy again.

| Status | HTTP Code |
| --- | --- | --- |
| Healthy | 200 |
| Unhealthy | 500 |

I use Healthttpd to provide endpoints for [Kubenetes liveness and readiness probes](https://kubernetes.io/docs/tasks/configure-pod-container/configure-liveness-readiness-probes/).

## Example Usage

Add the Healthttpd library to your `build.sbt`:

```
libraryDependencies += "mu.node" %% "healthttpd" % "0.1.0"
```

Start a Healthttpd server on port 8080, configure health check, and indicate ready status once the application is up and running.

```
object UserService extends App {

  // Start Healthttpd early in your application lifecycle
  val healthttpd = Healthttpd(8080)
    .setHealthCheck((): Boolean => {
      // This health check happens every time we access the /health endpoint
      Try(userRepository.canQueryUsers() && eventPublisher.canListTopicPartitions())
        .getOrElse(false)
    })
    // UserService is not yet ready to serve requests
    .startAndIndicateNotReady()

    lazy val userRepository = ???
    lazy val eventPublisher = ???

    // ...

    // Once our user service is up and serving requests
    healthttpd.indicateReady()

    sys.ShutdownHookThread {
      healthttpd.stop()
    }
}
```

Healthttpd is built on top of [NanoHTTPD](https://github.com/NanoHttpd/nanohttpd).