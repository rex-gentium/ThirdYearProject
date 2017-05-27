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
		System.out.println("Closing sessions and cleaning up storage space");
		WebInterfaceService.sessions.closeAllSessions();
		System.out.println("Closing database connection");
		WebInterfaceService.db.close();
		System.out.println("ServletContextListener destroyed");
	}

	//Run this before web application is started
	@Override
	public void contextInitialized(ServletContextEvent arg0) {
		System.out.println("ServletContextListener started");
		try {
			System.out.println("Registering database driver");
			WebInterfaceService.db = new DatabaseConnector(DatabaseConnector.EMBEDDED_DERBY_DRIVER,
					DatabaseConnector.DERBY_PROTOCOL, "CryptoANN");
			System.out.println("Opening database connection");
			WebInterfaceService.db.connect();
		} catch (Exception e) {
			System.out.println("Failed to connect to database!");
			e.printStackTrace();
		}
		System.out.println("Opening session pool");
		WebInterfaceService.sessions = new SessionPool();
	}
}
