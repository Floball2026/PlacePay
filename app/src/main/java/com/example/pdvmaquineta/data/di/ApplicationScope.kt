package com.example.pdvmaquineta.data.di

import javax.inject.Qualifier

// Escopo de corrotina vinculado ao processo do app (nao a uma tela). Usado
// para envios de rede que devem continuar mesmo que o operador saia da tela
// da venda logo apos finalizar.
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
