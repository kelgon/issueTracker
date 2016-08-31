package com.issuetracker.db;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.bson.Document;
import org.bson.types.ObjectId;

import com.issuetracker.util.PropertiesUtil;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientURI;
import com.mongodb.client.FindIterable;

/**
 * MongoDB操作工具类
 * @author kelgon
 *
 */
public class MongoDBUtil {
	private static MongoClient mClient;
	
	private static String dbName;
	
	private static final Logger log = Logger.getLogger(MongoDBUtil.class);
	
	//初始化MongoDB Client
	static {
		try {
			dbName = PropertiesUtil.getFileProp("mongodbname");
			String mongoUri = PropertiesUtil.getFileProp("mongodburi");
			int maxConn = Integer.parseInt(PropertiesUtil.getFileProp("mongodbmaxconn"));
			int minConn = Integer.parseInt(PropertiesUtil.getFileProp("mongodbminconn"));
		
			if (mClient == null) {
				log.info("初始化MongoDB连接...");
				mClient = new MongoClient(new MongoClientURI(mongoUri,
						new MongoClientOptions.Builder().socketKeepAlive(true)
								.connectTimeout(5000).connectionsPerHost(maxConn)
								.minConnectionsPerHost(minConn)));
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}
	
	/**
	 * 测试MongoDB连接可用性并输出Mongo集群状态
	 */
	public static void test() {
		log.info("测试MongoDB连接...");
		mClient.listDatabaseNames();
		log.info("测试完成");
	}

	/**
	 * 执行MongoDB find命令，返回单个结果
	 * @param collection 集合名
	 * @param filter 查询filter
	 * @return 包含有查询结果的Document
	 */
	public static Document findOne(String collection, Document filter) {
		FindIterable<Document> it = mClient.getDatabase(dbName).getCollection(collection).find(filter);
		return it.first();
	}

	/**
	 * 执行MongoDB find命令
	 * @param collection 集合名
	 * @param filter 查询filter
	 * @return 包含有查询结果的ArrayList
	 */
	public static List<Document> find(String collection, Document filter) {
		FindIterable<Document> it = null;
		if(filter == null)
			it = mClient.getDatabase(dbName).getCollection(collection).find();
		else
			it = mClient.getDatabase(dbName).getCollection(collection).find(filter);
		List<Document> list = new ArrayList<Document>();
		for(Document doc : it) {
			list.add(doc);
		}
		return list;
	}

	/**
	 * 执行MongoDB find命令（分页）
	 * @param collection 集合名
	 * @param filter 查询filter
	 * @param skip 跳过条目数
	 * @param limit 返回条目数
	 * @return 包含有查询结果的ArrayList
	 */
	public static List<Document> find(String collection, Document filter, Document sort, int skip, int limit) {
		FindIterable<Document> it = null;
		if(filter == null) {
			if(sort == null)
				it = mClient.getDatabase(dbName).getCollection(collection).find().skip(skip).limit(limit);
			else
				it = mClient.getDatabase(dbName).getCollection(collection).find().sort(sort).skip(skip).limit(limit);
		}
		else {
			if(sort == null)
				it = mClient.getDatabase(dbName).getCollection(collection).find(filter).skip(skip).limit(limit);
			else
				it = mClient.getDatabase(dbName).getCollection(collection).find(filter).sort(sort).skip(skip).limit(limit);
		}
		List<Document> res = new ArrayList<Document>();
		for(Document doc : it) {
			res.add(doc);
		}
		return res;
	}
	
	/** 
	 * 执行MongoDB count命令
	 * @param collection 集合名
	 * @param filter 查询filter
	 * @return 符合条件的记录数
	 */
	public static long count(String collection, String filter) {
		return mClient.getDatabase(dbName).getCollection(collection).count(Document.parse(filter));
	}
	
	/**
	 * 插入单条记录
	 * @param collection 集合名
	 * @param doc 记录对象
	 * @return 插入记录的ObjectId
	 */
	public static ObjectId insert(String collection, Document doc) {
		mClient.getDatabase(dbName).getCollection(collection).insertOne(doc);
		return doc.getObjectId("_id");
	}
	
	/**
	 * 批量插入数据
	 * @param collection 集合名
	 * @param docs 要插入的数据List
	 */
	public static void insertMany(String collection, List<Document> docs) {
		mClient.getDatabase(dbName).getCollection(collection).insertMany(docs);
	}
	
	/**
	 * 执行MongoDB update命令
	 * @param collection 集合名
	 * @param filter update条件
	 * @param update update内容
	 * @return 更新的数据条目数
	 */
	public static long update(String collection, Document filter, Document update) {
		return mClient.getDatabase(dbName).getCollection(collection).updateOne(filter, update).getModifiedCount();
	}
	
	/**
	 * 执行MongoDB deleteOne命令
	 * @param collection 集合名
	 * @param filter delete条件
	 * @return 删除的数据条目数
	 */
	public static long deleteOne(String collection, Document filter) {
		return mClient.getDatabase(dbName).getCollection(collection).deleteOne(filter).getDeletedCount();
	}
}
