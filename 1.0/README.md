## 源代码

1.0 的源码可以从官网下载，https://archive.apache.org/dist/commons/dbutils/source/commons-dbutils-1.0-src.zip

## 使用例子

一张用户表 `user`

```sql
CREATE TABLE user (
    id int NOT NULL AUTO_INCREMENT,
    name varchar(30),
    create_time datetime,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
```

### 1. 初始化数据连接池

使用数据连接池获取连接，这里使用 Druid 连接池

```java
public class TestJDBC {

    private static final String URL = "jdbc:mysql://127.0.0.1:8889/how2j?characterEncoding=UTF-8";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "root";

    private DataSource initDataSource() {
        DruidDataSource dataSource = new DruidDataSource();

        dataSource.setUrl(URL);
        dataSource.setUsername(USERNAME);
        dataSource.setPassword(PASSWORD);

        return dataSource;
    }
}
```

### 2. 初始化 QueryRunner

DbUtils 对数据库的 CRUD 操作都是使用 `QueryRunner` 类进行操作。要使用 `QueryRunner`，得传入一个 `javax.sql.DataSource`，在内部通过 `javax.sql.DataSource` 获取 `Connection`

```java
QueryRunner queryRunner = new QueryRunner(initDataSource());
```

有了 `QueryRunner` 之后，就可以进行数据库操作了。`INSERT`、`UPDATE`、`DELETE` 调用 `QueryRunner.update()` 方法，对于 `SELECT` 则使用 `QueryRunner.query()` 方法。

### 3. 插入100条用户记录

```java
/**
 * 插入100条用户记录
 */
public void insertUsers() {
    QueryRunner queryRunner = new QueryRunner(initDataSource());

    int insertCount = 0;
    try {
        for (int i = 1; i <= 100; i++) {
            int effect = queryRunner.update("INSERT INTO user (name, create_time) VALUES (?, ?)",
                    new Object[]{"user_" + i, new Date()});
            insertCount += effect;
        }
    } catch (SQLException e) {
        e.printStackTrace();
    }
    
    if (insertCount == 100) {
        System.out.println("插入100条记录成功");
    } else {
        System.out.println("插入100条记录失败");
    }
}
```

**注意：**

> DbUtils 1.0 目前没有一个独立的 `insert()` 方法来返回自增 id 字段，以后的版本可能会提供

### 4. 删除一条记录

```java
/**
 * 删除指定id的数据记录
 */
public void deleteById(int id) {
    QueryRunner queryRunner = new QueryRunner(initDataSource());

    try {
        int effect = queryRunner.update("DELETE FROM user WHERE id = ?", id);
        if (effect == 1) {
            System.out.println("删除记录成功");
        } else {
            System.out.println("删除记录失败");
        }
    } catch (SQLException e) {
        e.printStackTrace();
    }
}
```

### 5. 修改一条记录

```java
/**
 * 修改指定用户的用户名
 */
public void updateUser(int id, String newName) {
    QueryRunner queryRunner = new QueryRunner(initDataSource());

    try {
        int effect = queryRunner.update("UPDATE user SET name = ? WHERE id = ?",
                new Object[]{newName, id});
        if (effect == 1) {
            System.out.println("修改成功");
        } else {
            System.out.println("修改失败");
        }
    } catch (SQLException e) {
        e.printStackTrace();
    }
}
```

### 6. 查询数据库中记录的个数

```java
/**
 * 查询数据库中记录的个数
 */
public void getCount() {
    QueryRunner queryRunner = new QueryRunner(initDataSource());

    try {
        Long count = (Long) queryRunner.query("SELECT COUNT(*) FROM user", new ScalarHandler());
        System.out.println(count);
    } catch (SQLException e) {
        e.printStackTrace();
    }
}
```

### 7. 查询指定用户的用户名

```java
/**
 * 查询指定用户的用户名
 */
public void getName(int id) {
    QueryRunner queryRunner = new QueryRunner(initDataSource());

    try {
        Object name = queryRunner.query("SELECT * FROM user WHERE id = ?", 
                id, new ScalarHandler("name"));
        System.out.println(name);
    } catch (SQLException e) {
        e.printStackTrace();
    }
}
```

### 8. 获取指定用户的记录，并转换为 Java Bean

`user` 表的 JavaBean

```java
public class User {

    private Integer id;
    private String name;
    private Date create_time;

    // getter 方法
    // setter 方法
}
```

类的属性名要和表的字段名对上，否则无法映射成功

```java
/**
 * 获取指定用户的记录，并转换为 JavaBean
 */
public void getUserBeanById(int id) {
    QueryRunner queryRunner = new QueryRunner(initDataSource());

    try {
        User user = (User) queryRunner.query("SELECT * FROM user WHERE id = ?",
                id, new BeanHandler(User.class));
        System.out.println(user);
    } catch (SQLException e) {
        e.printStackTrace();
    }
}
```

### 9. 获取用户列表，并转换为 List<JavaBean>

```java
/**
 * 获取用户列表，并转换为 List<JavaBean>
 */
public void getUserListOfCount(int count) {
    QueryRunner queryRunner = new QueryRunner(initDataSource());

    try {
        List<User> userList = (List<User>) queryRunner.query("SELECT * FROM user LIMIT ?",
                count, new BeanListHandler(User.class));
        System.out.println(userList);
    } catch (SQLException e) {
        e.printStackTrace();
    }
}
```

### 10. 事务

```java
/**
 * 开启事务，修改用户名
 */
public void transaction() {
    Connection connection = null;
    try {
        // 获取connection
        connection = initDataSource().getConnection();
        // 开始事务操作前，先把自动提交关闭
        connection.setAutoCommit(false);

        QueryRunner queryRunner = new QueryRunner();
        // 传入connection
        queryRunner.update(connection, "UPDATE user SET name = 'updated_name' WHERE id = 5");
        int a = 1 / 0;  // 抛出异常，引起事务回滚
        
        // 最后再进行提交
        connection.commit();
    } catch (Exception e) {
        e.printStackTrace();
        
        try {
            DbUtils.rollback(connection);
        } catch (SQLException e1) {
            e1.printStackTrace();
        }
    }
}
```

## 11. 调用存储过程

目前 1.0 的版本没有调用存储过程的方法，以后的版本应该会提供

## 源码分析

### 核心类

- QueryRunner 类
- 各种各样的 ResultSetHandler 接口实现类
- DbUtils 类

### QueryRunner 类

QueryRunner 提供了两个构造方法

- `QueryRunner`
- `QueryRunner(DataSource ds)`

还提供了对数据库进行 CRUD 的两个方法

- 增、删、改：`update()`
- 查：`query()`

先从简单的 `update()` 的看起

```java
/**
 * Executes the given INSERT, UPDATE, or DELETE SQL statement.  The 
 * <code>Connection</code> is retrieved from the <code>DataSource</code> 
 * set in the constructor.
 * 
 * @param sql The SQL statement to execute.
 * @param params Initializes the PreparedStatement's IN (i.e. '?') 
 * parameters.
 * @throws SQLException
 * @return The number of rows updated.
 */
public int update(String sql, Object[] params) throws SQLException {

    Connection conn = this.ds.getConnection();

    try {
        return this.update(conn, sql, params);

    } finally {
        DbUtils.close(conn);
    }
}
```

方法内通过 `DataSource` 获取 `Connection`，然后再调用 `update(conn, sql, params)`

```java
/**
 * Execute an SQL INSERT, UPDATE, or DELETE query.
 * 
 * @param conn The connection to use to run the query.
 * @param sql The SQL to execute.
 * @param params The query replacement parameters.
 * @return The number of rows updated.
 * @throws SQLException
 */
 public int update(Connection conn, String sql, Object[] params)
    throws SQLException {

    PreparedStatement stmt = null;
    int rows = 0;

    try {
        // 1. 预编译 sql，获取 `PreparedStatement` 对象
        stmt = this.prepareStatement(conn, sql);
        // 2. 把参数和 `PreparedStatement` 对象绑定
        this.fillStatement(stmt, params);

        // 3. 调用 jdbc 的 `PreparedStatement.executeUpdate()` 方法执行 sql 语句
        rows = stmt.executeUpdate();

    } catch (SQLException e) {
        this.rethrow(e, sql, params);

    } finally {
        DbUtils.close(stmt);
    }

    return rows;
}
```

这个方法做了三件事

1. 预编译 sql，获取 `PreparedStatement` 对象
2. 把参数和 `PreparedStatement` 对象绑定
3. 调用 jdbc 的 `PreparedStatement.executeUpdate()` 方法执行 sql 语句

这三个步骤和我们平时写 JDBC 语句对步骤差不多，差别在于“把参数和 `PreparedStatement` 对象绑定”的实现

```java
/**
 * Fill the <code>PreparedStatement</code> replacement parameters with 
 * the given objects.
 * @param stmt
 * @param params Query replacement parameters; <code>null</code> is a valid
 * value to pass in.
 * @throws SQLException
 */
protected void fillStatement(PreparedStatement stmt, Object[] params)
    throws SQLException {

    if (params == null) {
        return;
    }

    for (int i = 0; i < params.length; i++) {
        if (params[i] != null) {
            stmt.setObject(i + 1, params[i]);
        } else {
            stmt.setNull(i + 1, Types.OTHER);
        }
    }
}
```

调用的是 `PreparedStatement.setObject` 和 `PreparedStatement.setNull` 方法，和我们平时的写法不同，我们是这么写

```java
String sql = "insert into user(username,password) values(?,?)";

ps = connection.prepareStatement(sql);
ps.setString(1, "王五");
ps.setString(2, "我是密码");

int rows = ps.executeUpdate();
```

如果用 `PreparedStatement.setObject` 和 `PreparedStatement.setNull` 方法对话，就不用根据参数的类型去调用对应的方法了，比较简单。

接着来看看比较复杂的 `query()` 方法

```java
/**
 * Executes the given SELECT SQL query and returns a result object.
 * The <code>Connection</code> is retrieved from the 
 * <code>DataSource</code> set in the constructor.
 * 
 * @param sql The SQL statement to execute.
 * @param params Initialize the PreparedStatement's IN parameters with
 * this array.
 * 
 * @param rsh The handler used to create the result object from 
 * the <code>ResultSet</code>.
 * 
 * @return An object generated by the handler.
 * @throws SQLException
 */
 public Object query(String sql, Object[] params, ResultSetHandler rsh)
    throws SQLException {

    Connection conn = this.ds.getConnection();

    try {
        return this.query(conn, sql, params, rsh);

    } finally {
        DbUtils.close(conn);
    }
}
```

这个方法需要传入三个参数：sql 语句、参数、`ResultSetHandler` 的实现类（比较常用的是 `BeanHandler`)。然后再调用 `query(conn, sql, params, rsh)` 方法

```java
public Object query(
    Connection conn,String sql, Object[] params, ResultSetHandler rsh)
    throws SQLException {

    PreparedStatement stmt = null;
    ResultSet rs = null;
    Object result = null;

    try {
        // 1. 预编译 sql，获取 `PreparedStatement` 对象
        stmt = this.prepareStatement(conn, sql);
        // 2. 把参数和 `PreparedStatement` 对象绑定
        this.fillStatement(stmt, params);

        // 3. 调用 jdbc 的 `PreparedStatement.executeUpdate()` 方法执行 sql 语句，获取 `ResultSet`
        rs = this.wrap(stmt.executeQuery());

        // 4. 利用 `ResultSetHandler` 转换 `ResultSet`
        result = rsh.handle(rs);

    } catch (SQLException e) {
        this.rethrow(e, sql, params);

    } finally {
        DbUtils.close(rs);
        DbUtils.close(stmt);
    }

    return result;
}
```

这个方法做了几件事：

1. 预编译 sql，获取 `PreparedStatement` 对象
2. 把参数和 `PreparedStatement` 对象绑定
3. 调用 jdbc 的 `PreparedStatement.executeUpdate()` 方法执行 sql 语句，获取 `ResultSet`
4. 利用 `ResultSetHandler` 转换 `ResultSet`

核心代码是在利用 `ResultSetHandler` 转换 `ResultSet` 的部分，点进去就跳转到 `BeanHandler` 的 `handle(ResultSet rs)` 方法。这个方法是实现了 `ResultSetHandler` 接口的 `handle(ResultSet rs)` 方法

```java
/**
 * Convert the first row of the <code>ResultSet</code> into a bean with the
 * <code>Class</code> given in the constructor.
 * 
 * @return An initialized JavaBean or <code>null</code> if there were no 
 * rows in the <code>ResultSet</code>.
 * 
 * @throws SQLException
 * @see org.apache.commons.dbutils.ResultSetHandler#handle(ResultSet)
 */
public Object handle(ResultSet rs) throws SQLException {
    return rs.next() ? this.convert.toBean(rs, this.type) : null;
}
```

既然 `BeanHandler` 类这么重要，看看内部的实现吧。其实 `BeanHandler` 的代码很少

```java
public class BeanHandler implements ResultSetHandler {

    private Class type = null;

    private RowProcessor convert = BasicRowProcessor.instance();

    public BeanHandler(Class type) {
        this.type = type;
    }

    public BeanHandler(Class type, RowProcessor convert) {
        this.type = type;
        this.convert = convert;
    }

    public Object handle(ResultSet rs) throws SQLException {
        return rs.next() ? this.convert.toBean(rs, this.type) : null;
    }
}
```

提供两个构造方法

- `BeanHandler(Class type)`
- `BeanHandler(Class type, RowProcessor convert)`

不过第二个构造方法用不着，用第一个就行，`RowProcessor` 使用默认的 `BasicRowProcessor` 即可。比如

```java
User user = (User) queryRunner.query("SELECT * FROM user WHERE id = ?",
        id, new BeanHandler(User.class));
```

在 `handle(ResultSet rs)` 方法中，会利用 `BasicRowProcessor` 的 `toBean()` 方法对传入的 Java Bean Class 进行转换

```java
/**
 * Convert a <code>ResultSet</code> row into a JavaBean.  This 
 * implementation uses reflection and <code>BeanInfo</code> classes to 
 * match column names to bean property names.  Properties are matched to 
 * columns based on several factors:
 * <br/>
 * <ol>
 *     <li>
 *     The class has a writable property with the same name as a column.
 *     The name comparison is case insensitive.
 *     </li>
 * 
 *     <li>
 *     The property's set method parameter type matches the column 
 *     type. If the data types do not match, the setter will not be called.
 *     </li>
 * </ol>
 * 
 * <p>
 * Primitive bean properties are set to their defaults when SQL NULL is
 * returned from the <code>ResultSet</code>.  Numeric fields are set to 0
 * and booleans are set to false.  Object bean properties are set to 
 * <code>null</code> when SQL NULL is returned.  This is the same behavior
 * as the <code>ResultSet</code> get* methods.
 * </p>
 * 
 * @see RowProcessor#toBean(ResultSet, Class)
 */
public Object toBean(ResultSet rs, Class type) throws SQLException {

    PropertyDescriptor[] props = this.propertyDescriptors(type);

    ResultSetMetaData rsmd = rs.getMetaData();

    int[] columnToProperty = this.mapColumnsToProperties(rsmd, props);

    int cols = rsmd.getColumnCount();

    return this.createBean(rs, type, props, columnToProperty, cols);
}
```

首先说一下这个方法的原理：利用反射，将查询结果映射到 Java Bean 中。然后接着说实现细节

通过 Java Bean 内省，获取 属性描述符（PropertyDescriptor）

```java
PropertyDescriptor[] props = this.propertyDescriptors(type);
```

对于 `User` 类来说，就是

* id
* name
* create_time

获取查询结果的元数据

```java
ResultSetMetaData rsmd = rs.getMetaData();
```

获取字段的 columnIndex 数组

```java
int[] columnToProperty = this.mapColumnsToProperties(rsmd, props);
```

经过以上步骤后，就来到重头戏

```java
this.createBean(rs, type, props, columnToProperty, cols);
```

这个方法就是创建 Java Bean 对象了

```java
/**
 * Creates a new object and initializes its fields from the ResultSet.
 *
 * @param rs The result set
 * @param type The bean type (the return type of the object)
 * @param props The property descriptors
 * @param columnToProperty The column indices in the result set
 * @param cols The number of columns
 * @return An initialized object.
 * @throws SQLException If a database error occurs
 */
private Object createBean(ResultSet rs, Class type, PropertyDescriptor[] props,
    int[] columnToProperty, int cols) throws SQLException {

    Object bean = this.newInstance(type);

    for (int i = 1; i <= cols; i++) {

        if (columnToProperty[i] == PROPERTY_NOT_FOUND) {
            continue;
        }
        
        Object value = rs.getObject(i);

        PropertyDescriptor prop = props[columnToProperty[i]];
        Class propType = prop.getPropertyType();

        if (propType != null && value == null && propType.isPrimitive()) {
            value = primitiveDefaults.get(propType);
        }

        this.callSetter(bean, prop, value);
    }

    return bean;
}
```

实现细节其实就是调用属性的 getter 方法设置值，其他没有了。所以 `User` 类一定要有 setter 方法，否则没办法转换 Java Bean

## 总结

以上就是对 DbUtils 1.0 源码的分析。DbUtils 帮我们做了几件事

- Connection 的管理
- ResultSet 查询结果的映射

缺点：

* 无法调用存储过程
* 不支持 SQL 中字段的别名，比如 `SELECT userid AS id FROM osc_users`
* 不支持泛型