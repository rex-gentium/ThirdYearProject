package edu.susu.crypto;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import edu.susu.database.DatabaseConnector;

public class ServiceContextListener implements ServletContextListener{

	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
		System.out.println("ServletContextListener destroyed");
		//WebInterfaceService.db.close();
	}

    //Run this before web application is started
	@Override
	public void contextInitialized(ServletContextEvent arg0) {
		System.out.println("ServletContextListener started");
		/*try {
			WebInterfaceService.db = new DatabaseConnector(DatabaseConnector.EMBEDDED_DERBY_DRIVER,
					DatabaseConnector.DERBY_PROTOCOL, "CryptoANN");
		} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
			System.out.println("Failed to connect to database!");
			e.printStackTrace();
		}*/
	}
}
