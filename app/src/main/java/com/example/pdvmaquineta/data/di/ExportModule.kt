package com.example.pdvmaquineta.data.di

import com.example.pdvmaquineta.data.export.AndroidReportExporter
import com.example.pdvmaquineta.domain.export.ReportExporter
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ExportModule {

    @Binds
    @Singleton
    abstract fun bindReportExporter(impl: AndroidReportExporter): ReportExporter
}
