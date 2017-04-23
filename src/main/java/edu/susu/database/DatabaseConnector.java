package edu.susu.database;

import java.sql.*;

import edu.susu.exception.IdleUpdateException;

/**
 * JDBC-коннектор к локальной базе данных пользователей
 * @author Carolus
 *
 */
public class DatabaseConnector {
	
	private boolean debugMode = true;
	public final static String EMBEDDED_DERBY_DRIVER = "org.apache.derby.jdbc.EmbeddedDriver";
	public final static String DERBY_PROTOCOL = "jdbc:derby:";
	
	private final String driver;
	private final String protocol;
	private final String dbName;
	private Connection connection;
	
	/**
	 * Создаёт объект коннектора к указанной базе данных и регистрирует встроенный драйвер
	 * @param driverName название (встроенного) драйвера базы данных
	 * @param protocol проткол подключения к базе данных
	 * @param databaseName название базы данных
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws ClassNotFoundException
	 */
	public DatabaseConnector(String driverName, String protocol, String databaseName) 
			throws InstantiationException, IllegalAccessException, ClassNotFoundException 
	{
		this.driver = driverName;
		this.protocol = protocol;
		this.dbName = databaseName;
		registerDerbyDriverInstance();
	}
	
	/**
	 * Регистрирует втроенный драйвер базы данных
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws ClassNotFoundException
	 */
	public void registerDerbyDriverInstance() 
			throws InstantiationException, IllegalAccessException, ClassNotFoundException 
	{
		Class.forName(driver).newInstance();
		if (debugMode)
			System.out.println("Registered database driver instance");
	}
	
	/**
	 * Осуществляет подключение к базе данных
	 * @throws SQLException
	 */
	public void connect() throws SQLException 
	{
		connection = DriverManager.getConnection(protocol + dbName + ";create=true");
		if (debugMode)
			System.out.println("Connected to / created database " + dbName);
		if (isDatabaseEmpty()) {
			createTables();
			if (debugMode)
				System.out.println("Created tables");
		}
	}

	/**
	 * Проверка, не закрыто ли соединение с базой данных
	 * @return false, если соединение закрыто, иначе true
	 */
	public boolean isConnected() {
		try {
			return !connection.isClosed();
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	/**
	 * Закрывает соединение с базой данных
	 */
	public void close() {
		try {
			if (isConnected())
				connection.close();
			if (debugMode)
				System.out.println("Disconnected from database");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Проверяет, есть ли в базе таблицы, выводит их в консоль отладки
	 * @return true если база не пуста, иначе false
	 * @throws SQLException
	 */
	public boolean isDatabaseEmpty() throws SQLException {
		boolean result = true;
		ResultSet res = connection.getMetaData().getTables(null, null, null, new String[] {"TABLE"});
		if (debugMode)
			System.out.println("List of tables: "); 
		if (res.next()) {
			result = false;
			if (debugMode)
				System.out.println(res.getString(3));
		}
		res.close();
		return result;
	}
	
	/**
	 * Создаёт новые таблицы в базе данных
	 * @throws SQLException в случае ошибки при создании одной из таблиц успешно созданные удаляются
	 */
	private void createTables() throws SQLException {
		String createTableUsers = "CREATE TABLE Users"
				+ "("
				+ "username VARCHAR(60) NOT NULL,"
				+ "password_hash CHAR(32) FOR BIT DATA,"
				+ "ann_config_path VARCHAR(1000),"
				+ "PRIMARY KEY (username)"
				+ ")";
		try {
			Statement statement = connection.createStatement();
			System.out.println("Creating Table Users");
			statement.executeUpdate(createTableUsers);
		} catch (SQLException sqle) {
			connection.rollback();
			throw sqle;
		}
		connection.commit();
	}
	
	/**
	 * Удаляет все таблицы из базы данных
	 * @throws SQLException в случае ошибки при удалении одной из таблиц все таблицы восстанавливаются
	 */
	public void clear() throws SQLException {
		String[] sqls = { "DROP TABLE Users" };
		for (String sql : sqls)
			try {
				connection.createStatement().executeUpdate(sql);
				if (debugMode)
					System.out.print(sql);
			} catch (SQLException sqle) {
				System.out.println(sqle.getSQLState());
				// 42Y55 - STATEMENT cannot be performed on TABLE because it does not exist
				if (sqle.getSQLState() != "42Y55") {
					connection.rollback();
					throw sqle;
				}
			}
	}
	
	/**
	 * Регистрация нового пользователя
	 * @param name
	 * @param pswdHash
	 * @throws SQLException
	 */
	public void addUser(String name, byte[] pswdHash) throws SQLException {
		final String sql = "INSERT INTO USERS(username, password_hash, ann_config_path) VALUES(?, ?, ?)";
		final PreparedStatement preparedStatement = connection.prepareStatement(sql);
		preparedStatement.setString(1, name);
		preparedStatement.setBytes(2, pswdHash);
		preparedStatement.setNull(3, Types.VARCHAR);
		preparedStatement.executeUpdate();
		connection.commit();
	}

	/**
	 * Получение представления пользователя
	 * @param name
	 * @return представление пользователя, null если пользователя не существует
	 * @throws SQLException
	 */
	public User getUser(String name) throws SQLException {
		User result = null;
		final String sql = "SELECT * FROM Users WHERE username = \'" + name + "\'";
		ResultSet rs = connection.createStatement().executeQuery(sql);
		if (rs.next()) {
			result = new User();
			result.setUsername(rs.getString("username"));
			result.setPasswordHash(rs.getBytes("password_hash"));
			result.setNeuralNetworkConfigFilePath(rs.getString("ann_config_path"));
		}
		return result;
	}
	
	/**
	 * Обновление пароля пользователя
	 * @param name
	 * @param newPswdHash
	 * @throws SQLException
	 * @throws IdleUpdateException если пользователя не существует или новый пароль совпадает со старым
	 */
	public void updateUserPassword(String name, byte[] newPswdHash) throws SQLException, IdleUpdateException 
	{
		StringBuilder hexString = new StringBuilder("x\'");
		for(byte b : newPswdHash)
			hexString.append(String.format("%02x", b));
		String newPswdHashHexString = hexString.append('\'').toString(); // byte array in string format, like x'4f33901a'
		String sql = "UPDATE Users SET password_hash = " + newPswdHashHexString + " WHERE username = \'" + name + "\'";
		int result = connection.createStatement().executeUpdate(sql);
		if (result == 0)
			throw new IdleUpdateException("No updates where made");
		else 
			connection.commit();
	}

	/**
	 * Обновление файла настроек криптографических средств пользователя
	 * @param name
	 * @param newFilePath
	 * @throws SQLException
	 * @throws IdleUpdateException если пользователя не существует или новое имя файла совпадает со старым
	 */
	public void updateUserConfigFile(String name, String newFilePath) throws SQLException, IdleUpdateException {
		String sql = "UPDATE Users SET ann_config_path = \'" + newFilePath	+ "\' WHERE username = \'" + name + "\'";
		int result = connection.createStatement().executeUpdate(sql);
		if (result == 0)
			throw new IdleUpdateException("No updates where made");
		else 
			connection.commit();
	}

	/**
	 * Удаление учетной записи пользователя
	 * @param name
	 * @throws SQLException
	 * @throws IdleUpdateException если пользователя не существует (нечего удалять)
	 */
	public void deleteUser(String name) throws SQLException, IdleUpdateException {
		String sql = "DELETE FROM Users WHERE username = \'" + name + "\'";
		int result = connection.createStatement().executeUpdate(sql);
		if (result == 0)
			throw new IdleUpdateException("No deletes where made");
		else
			connection.commit();
	}
	
}
