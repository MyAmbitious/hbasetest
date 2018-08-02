package com.zzy.hbasetest;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.filter.CompareFilter.CompareOp;
import org.apache.hadoop.hbase.filter.*;
import org.apache.hadoop.hbase.util.Bytes;

import java.util.ArrayList;
import java.util.List;

/**
 * @ClassName: HelloFilterListNestingQuery
 * @description: 嵌套查询
 * @author: 赵正阳
 * @date: 2018-07-27 10:25
 * @version: V1.0
 **/
public class HelloFilterListNestingQuery {


    private static byte[] printResult(ResultScanner rs) {
        byte[] lastRowKey = null;
        for (Result r : rs) {
            byte[] rowkey = r.getRow();
            String name = Bytes.toString(r.getValue(Bytes.toBytes("mycf"), Bytes.toBytes("name")));
            String city = Bytes.toString(r.getValue(Bytes.toBytes("mycf"), Bytes.toBytes("city")));
            String active = Bytes.toString(r.getValue(Bytes.toBytes("mycf"), Bytes.toBytes("active")));
            System.out.println(Bytes.toString(rowkey) + ": name=" + name + " city=" + city + " active=" + active);
            lastRowKey = rowkey;
        }
        return lastRowKey;
    }


    public static void main(String[] args) throws Exception {
        // 获取配置文件
        Configuration config = HBaseConfiguration.create();

        // 添加必要的配置文件 (hbase-site.xml, core-site.xml)
        config.addResource(new Path(ClassLoader.getSystemResource("hbase-site.xml").toURI()));
        config.addResource(new Path(ClassLoader.getSystemResource("core-site.xml").toURI()));

        try (Connection connection = ConnectionFactory.createConnection(config)) {
            Table table = connection.getTable(TableName.valueOf("mytable"));
            Scan scan = new Scan();

            List<Filter> innerFilters = new ArrayList<>();

            // 找出住在 xiamen 的人
            Filter xiamenFilter = new SingleColumnValueFilter(Bytes.toBytes("mycf"), Bytes.toBytes("city"), CompareOp.EQUAL, new BinaryComparator(Bytes.toBytes("xiamen")));
            innerFilters.add(xiamenFilter);

            // 找出住在 shanghai 的人
            Filter shanghaiFilter = new SingleColumnValueFilter(Bytes.toBytes("mycf"), Bytes.toBytes("city"), CompareOp.EQUAL, new BinaryComparator(Bytes.toBytes("shanghai")));
            innerFilters.add(shanghaiFilter);

            // 创建内层 FilterList，设置运算符为 OR
            FilterList innerFilterList = new FilterList(FilterList.Operator.MUST_PASS_ONE, innerFilters);

            // 外层查询过滤器列表
            List<Filter> outerFilters = new ArrayList<>();

            // 将内层过滤器列表作为外层过滤器列表的第一个过滤器
            outerFilters.add(innerFilterList);

            // 设置过滤条件为 active = '1'
            Filter activeFilter = new SingleColumnValueFilter(Bytes.toBytes("mycf"), Bytes.toBytes("active"), CompareOp.EQUAL, new BinaryComparator(Bytes.toBytes("1")));
            outerFilters.add(activeFilter);

            // 创建外层 FilterList，设置运算符为 AND
            FilterList outerFilterList = new FilterList(FilterList.Operator.MUST_PASS_ALL, outerFilters);

            scan.setFilter(outerFilterList);

            // 执行查询
            ResultScanner rs = table.getScanner(scan);
            printResult(rs);
            rs.close();
        }
    }
}
