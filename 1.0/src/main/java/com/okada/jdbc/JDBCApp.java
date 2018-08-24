package com.okada.jdbc;

import com.alibaba.druid.pool.DruidDataSource;
import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.apache.commons.dbutils.handlers.ScalarHandler;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

public class JDBCApp {

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

    public static void main(String[] args) {
        JDBCApp app = new JDBCApp();

        app.getUserBeanById(6);
    }
}