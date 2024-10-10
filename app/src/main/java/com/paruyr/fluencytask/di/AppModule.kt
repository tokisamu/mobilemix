package com.paruyr.fluencytask.di

import com.paruyr.fluencytask.data.BluetoothRepository
import com.paruyr.fluencytask.data.BluetoothRepositoryImpl
import com.paruyr.fluencytask.domain.usecase.SendMessageUseCase
import com.paruyr.fluencytask.presentation.viewmodel.BluetoothViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val bluetoothModule = module {
    single<BluetoothRepository> {
        BluetoothRepositoryImpl(
            context = androidContext()
        )
    }

    factory { SendMessageUseCase(get()) }

    viewModel {
        BluetoothViewModel(
            application = androidApplication(),
            sendMessageUseCase = get(),
            bluetoothRepository = get()
        )
    }
}
