package com.please.stop.app.features.categories.di

import com.please.stop.app.core.db.AppDatabase
import com.please.stop.app.di.dispatchers.DispatchersQualifiers
import com.please.stop.app.features.categories.data.repository.CategoriesRepositoryImpl
import com.please.stop.app.features.categories.domain.repository.CategoriesRepository
import com.please.stop.app.features.categories.domain.usecase.AddCategoryUseCase
import com.please.stop.app.features.categories.domain.usecase.AddSubcategoryUseCase
import com.please.stop.app.features.categories.domain.usecase.DeleteCategoryUseCase
import com.please.stop.app.features.categories.domain.usecase.DeleteSubcategoryUseCase
import com.please.stop.app.features.categories.domain.usecase.ObserveCategoriesUseCase
import com.please.stop.app.features.categories.domain.usecase.UpdateCategoryUseCase
import com.please.stop.app.features.categories.presentation.CategoriesStateHolder
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

val categoriesModule = module {

    single<CategoriesRepository> {
        CategoriesRepositoryImpl(
            categoryDao = get<AppDatabase>().categoryDao(),
            subcategoryDao = get<AppDatabase>().subcategoryDao(),
            expenseDao = get<AppDatabase>().expenseDao(),
            featureFlags = get(),
            ioDispatcher = get(named(DispatchersQualifiers.IO.name)),
        )
    }

    factory { ObserveCategoriesUseCase(repository = get()) }

    factory {
        AddCategoryUseCase(
            repository = get(),
            ioDispatcher = get(named(DispatchersQualifiers.IO.name)),
        )
    }

    factory {
        AddSubcategoryUseCase(
            repository = get(),
            ioDispatcher = get(named(DispatchersQualifiers.IO.name)),
        )
    }

    factory {
        UpdateCategoryUseCase(
            repository = get(),
            ioDispatcher = get(named(DispatchersQualifiers.IO.name)),
        )
    }

    factory {
        DeleteCategoryUseCase(
            repository = get(),
            ioDispatcher = get(named(DispatchersQualifiers.IO.name)),
        )
    }

    factory {
        DeleteSubcategoryUseCase(
            repository = get(),
            ioDispatcher = get(named(DispatchersQualifiers.IO.name)),
        )
    }

    viewModel {
        CategoriesStateHolder(
            observeCategoriesUseCase = get(),
            addCategoryUseCase = get(),
            addSubcategoryUseCase = get(),
            updateCategoryUseCase = get(),
            deleteCategoryUseCase = get(),
            deleteSubcategoryUseCase = get(),
        )
    }
}
