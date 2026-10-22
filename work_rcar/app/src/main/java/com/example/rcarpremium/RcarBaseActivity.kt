package com.example.rcarpremium

import androidx.appcompat.app.AppCompatActivity

/**
 * Base de navegação do RCarPremium.
 *
 * Nunca desenha botões "Voltar" em overlay: cada tela controla seu próprio
 * retorno (quando necessário), e o gesto/botão Voltar do Android continua ativo.
 * Isso evita duplicações e que botões cubram listas, formulários ou rodapés.
 */
open class RcarBaseActivity : AppCompatActivity()
