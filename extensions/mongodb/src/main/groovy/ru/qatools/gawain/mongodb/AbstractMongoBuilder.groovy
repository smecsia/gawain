package ru.qatools.gawain.mongodb
import com.mongodb.MongoClient
import com.mongodb.MongoClientURI
/**
 * @author Ilya Sadykov
 */
abstract class AbstractMongoBuilder {
    final String dbName
    final MongoClient mongoClient

    AbstractMongoBuilder(String connString, String dbName) {
        this.dbName = dbName
        this.mongoClient = new MongoClient(new MongoClientURI(connString))
    }

    AbstractMongoBuilder(MongoClient mongoClient, String dbName) {
        this.mongoClient = mongoClient
        this.dbName = dbName
    }
}
