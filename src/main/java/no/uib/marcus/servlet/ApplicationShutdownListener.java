package no.uib.marcus.servlet;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import no.uib.marcus.client.ElasticsearchClientFactory;

@WebListener
public class ApplicationShutdownListener implements ServletContextListener {

  @Override
  public void contextInitialized(ServletContextEvent sce) {
    // Called when application starts or is deployed.
  }

  @Override
  public void contextDestroyed(ServletContextEvent sce) {
    // Called on shutdown or undeploy.
    ElasticsearchClientFactory.closeClient(); // Gracefully close resources here
  }
}