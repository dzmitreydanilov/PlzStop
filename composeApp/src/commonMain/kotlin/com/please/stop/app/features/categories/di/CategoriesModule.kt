package com.please.stop.app.features.categories.di

import com.please.stop.app.core.db.AppDatabase
import com.please.stop.app.di.dispatchers.DispatchersQualifiers
import com.please.stop.app.features.categories.data.repository.CategoriesRepositoryImpl
import com.please.stop.app.features.categories.domain.repository.CategoriesRepository
import com.please.stop.app.features.categories.domain.usecase.AddCategoryUseCase
import com.please.stop.app.features.categories.domain.usecase.AddSubcategoryUseCase
import com.please.stop.app.features.categories.domain.usecase.ArchiveCategoryUseCase
import com.please.stop.app.features.categories.domain.usecase.ArchiveSubcategoryUseCase
import com.please.stop.app.features.categories.domain.usecase.DeleteSubcategoryUseCase
import com.please.stop.app.features.categories.domain.usecase.ObserveArchivedCategoriesUseCase
import com.please.stop.app.features.categories.domain.usecase.ObserveCategoriesUseCase
import com.please.stop.app.features.categories.domain.usecase.UnarchiveCategoryUseCase
import com.please.stop.app.features.categories.domain.usecase.UpdateCategoryUseCase
import com.please.stop.app.features.categories.presentation.CategoriesStateHolder
import com.please.stop.app.features.categories.presentation.archived.ArchivedCategoriesStateHolder
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

val categoriesModule = module {

    single<CategoriesRepository> {
        CategoriesRepositoryImpl(
            categoryDao = get<AppDatabase>().categoryDao(),
            subcategoryDao = get<AppDatabase>().subcategoryDao(),
            featureFlags = get(),
            ioDispatcher = get(named(DispatchersQualifiers.IO.name)),
        )
    }

    factory { ObserveCategoriesUseCase(repository = get()) }

    factory { ObserveArchivedCategoriesUseCase(repository = get()) }

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
        ArchiveCategoryUseCase(
            repository = get(),
            ioDispatcher = get(named(DispatchersQualifiers.IO.name)),
        )
    }

    factory {
        ArchiveSubcategoryUseCase(
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

    factory {
        UnarchiveCategoryUseCase(
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
            archiveCategoryUseCase = get(),
            deleteSubcategoryUseCase = get(),
        )
    }

    viewModel {
        ArchivedCategoriesStateHolder(
            observeArchivedCategoriesUseCase = get(),
            unarchiveCategoryUseCase = get(),
        )
    }
}
