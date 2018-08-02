package com.quorum.tessera;

import com.quorum.tessera.api.Tessera;
import com.quorum.tessera.config.Config;
import com.quorum.tessera.config.ServerConfig;
import com.quorum.tessera.config.cli.CliDelegate;
import com.quorum.tessera.config.cli.CliResult;
import com.quorum.tessera.grpc.server.GrpcServer;
import com.quorum.tessera.server.RestServer;
import com.quorum.tessera.server.RestServerFactory;
import com.quorum.tessera.service.locator.ServiceLocator;
import io.grpc.BindableService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

/**
 * The main entry point for the application. This just starts up the application
 * in the embedded container.
 */
public class Launcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(Launcher.class);

    public static void main(final String... args) throws Exception {

        try {
            final CliResult cliResult = CliDelegate.instance().execute(args);

            if(cliResult.isHelpOn()) {
                System.exit(0);
            } else if(cliResult.getStatus() != 0) {
                System.exit(cliResult.getStatus());
            }

            Config config = cliResult.getConfig().get();

            final URI uri = new URI(config.getServerConfig().getHostName() + ":" + config.getServerConfig().getPort());

//            runWebServer(uri, config.getServerConfig());

            runGrpcServer(uri);

            System.exit(0);

        } catch(ConstraintViolationException ex) {
            Set<ConstraintViolation<?>> violations = ex.getConstraintViolations();

            for(ConstraintViolation<?> violation : violations) {
                System.out.println("Config validation issue: " + violation.getPropertyPath() + " " + violation.getMessage());
            }
            System.exit(1);
        }
    }

    private static void runWebServer(final URI serverUri, ServerConfig serverConfig) throws Exception {

        final Tessera tessera = new Tessera(ServiceLocator.create(), "tessera-spring.xml");

        final RestServer restServer = RestServerFactory.create().createServer(serverUri, tessera, serverConfig);

        CountDownLatch countDown = new CountDownLatch(1);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                restServer.stop();
            } catch (Exception ex) {
                LOGGER.error(null, ex);
            } finally {
                countDown.countDown();
            }
        }));

        restServer.start();

        countDown.await();
    }

    private static void runGrpcServer(URI serverUri) throws IOException, InterruptedException {

        final Tessera tessera = new Tessera(ServiceLocator.create(), "tessera-spring.xml");

        final List<BindableService> services =
            tessera.getSingletons().stream()
                .filter(o -> o.getClass().getPackage().getName().startsWith("com.quorum.tessera.api.grpc"))
                .map(o -> (BindableService) o)
                .collect(Collectors.toList());

        final GrpcServer grpcServer = new GrpcServer(serverUri, services);

        grpcServer.start();

        grpcServer.blockUntilShutdown();

    }

}
