//> using scala "3.3.3"
//> using dep mysql:mysql-connector-java:8.0.33

import com.mysql.cj.jdbc.*

val dataSource = new MysqlDataSource()
dataSource.setServerName("127.0.0.1")
dataSource.setPortNumber(13306)
dataSource.setUser("ldbc")
dataSource.setPassword("password")
dataSource.setUseSSL(false) // v8 (caching_sha2_password) 使用の場合
dataSource.setAllowPublicKeyRetrieval(true) // v8 (caching_sha2_password) 使用の場合

// 接続
val connection = dataSource.getConnection

// 切断
connection.close()
