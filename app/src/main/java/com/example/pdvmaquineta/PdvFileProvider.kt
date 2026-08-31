package com.example.pdvmaquineta

import androidx.core.content.FileProvider

// Subclasse trivial só pra dar um nome de classe único ao nosso FileProvider.
// O SDK da PayTime (tectoy-lib) também declara um androidx.core.content.FileProvider,
// e dois providers com o MESMO android:name colidem no merge do manifesto.
// Com nomes de classe diferentes, os dois coexistem — cada um com sua authority.
// Nada muda no uso: o código continua usando a authority "${applicationId}.fileprovider".
class PdvFileProvider : FileProvider()
