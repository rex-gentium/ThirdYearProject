package edu.susu.crypto;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import edu.susu.database.DatabaseConnector;

import java.sql.SQLException;

/**
 * Обработчик, запускающийся перед веб-сервисом. Инициализирует подключение к базе данных
 */
public class ServiceContextListener implements ServletContextListener {

	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
		System.out.println("ServletContextListener destroyed");
		WebInterfaceService.db.close();
	}

	//Run this before web application is started
	@Override
	public void contextInitialized(ServletContextEvent arg0) {
		System.out.println("ServletContextListener started");
		try {
			WebInterfaceService.db = new DatabaseConnector(DatabaseConnector.EMBEDDED_DERBY_DRIVER,
					DatabaseConnector.DERBY_PROTOCOL, "CryptoANN");
			WebInterfaceService.db.connect();
		} catch (Exception e) {
			System.out.println("Failed to connect to database!");
			e.printStackTrace();
		}
	}
}
