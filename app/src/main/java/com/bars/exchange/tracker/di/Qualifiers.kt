package com.bars.exchange.tracker.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class LocalDataSourceAnnotation

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class RemoteDataSourceAnnotation
