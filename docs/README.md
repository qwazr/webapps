QWAZR Webapps
=============

With this library you can build fast and lightweight [micro-services](https://en.wikipedia.org/wiki/Microservices)
in JAVA 8 using [Servlets](https://en.wikipedia.org/wiki/Java_servlet)
and [JAX-RS](https://en.wikipedia.org/wiki/Java_API_for_RESTful_Web_Services).

QWAZR Webapps brings together a set of robust and efficient components:

- [Undertow](http://undertow.io/)
- [Jersey](https://jersey.github.io/)
- [Swagger](https://swagger.io/)
- [Webjars](http://www.webjars.org/)

Quickstart
----------

... (write in progress) ...

How to write a Web application
------------------------------

### The application main

```java
import com.qwazr.server.GenericServer;
import com.qwazr.server.GenericServerBuilder;
import com.qwazr.server.configuration.ServerConfiguration;
import com.qwazr.webapps.WebappManager;

import javax.management.JMException;
import javax.servlet.ServletException;
import java.io.IOException;

public class MyApplication {

	static GenericServer serverInstance;

	public static void main(String[] args)
			throws IOException, ReflectiveOperationException, ServletException, JMException {

		// Build the configuration of the server
		final ServerConfiguration configuration =
				ServerConfiguration.of().listenAddress("127.0.0.1").webAppPort(8080).build();

		// This is the generic server builder
		final GenericServerBuilder builder = GenericServer.of(configuration);

		// The web application definition
		final WebappManager webAppManager = WebappManager.of(builder, builder.getWebAppContext())
				.registerDefaultFaviconServlet()
				.registerWebjars() // Automatically mount all the webjars at /webjars/...
				.registerJavaServlet(MyServlet.class, () -> new MyServlet(
						"Hello World")) // Create a new servlet and inject dependencies thru the constructor
				.build();

		// Build and start the server
		serverInstance = builder.build();
		serverInstance.start(true);
	}
}
```

### A servlet with dependency injection thru the constructor

```java
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/test")
public class MyServlet extends HttpServlet {

	private final String text;

	MyServlet(final String text) {
		this.text = text;
	}

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		response.setContentType("text/html");
		response.getWriter().println("<html>" + text + "</html>");
	}
}
```

### The pom dependencies

```xml
<project>

    <dependency>
        <groupId>com.qwazr</groupId>
        <artifactId>qwazr-webapps</artifactId>
        <version>1.4.0-SNAPSHOT</version>
    </dependency>
    <dependency>
        <groupId>org.webjars</groupId>
        <artifactId>bootstrap</artifactId>
        <version>4.0.0-beta.3</version>
    </dependency>

<!-- We provide a BOM to handle dependencies conflicts -->
    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>com.qwazr</groupId>
                <artifactId>qwazr-bom</artifactId>
                <version>1.4.0-SNAPSHOT</version>
                <scope>import</scope>
                <type>pom</type>
            </dependency>
        </dependencies>
    </dependencyManagement>

<!-- Currently the library is a snapshot repository -->
    <repositories>
        <repository>
            <id>snapshots-repo</id>
            <url>https://oss.sonatype.org/content/repositories/snapshots</url>
            <releases>
                <enabled>false</enabled>
            </releases>
            <snapshots>
                <enabled>true</enabled>
            </snapshots>
        </repository>
        <repository>
            <id>unknown-jars-temp-repo</id>
            <name>A temporary repository created by NetBeans for libraries and jars it could not identify. Please
            replace the dependencies in this repository with correct ones and delete this repository.
            </name>
            <url>file:${project.basedir}/lib</url>
        </repository>
    </repositories>

</project>
```