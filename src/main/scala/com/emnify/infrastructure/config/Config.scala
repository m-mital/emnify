package com.emnify.infrastructure.config

case class DbConfig(username: String, password: String, url: String, migrateOnStart: Boolean)

case class Config(db: DbConfig, dataSource: String, closeOfferAggregationThreshold: Int, timeAggregationRate: Long)
