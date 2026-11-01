# ytslim — instruções para o Claude

Cliente do YouTube pessoal e enxuto: um único APK para celular, tablet e Android TV. Usa o MediaServiceCore do SmartTube como motor.

**Leia o `PLANO.md` antes de qualquer trabalho.** Ele tem as decisões do usuário, as telas, os filtros, o player, as etapas e as notas técnicas. Quando uma decisão mudar ou uma etapa for concluída, atualize o `PLANO.md` no mesmo commit.

## Regras

- **Converse com o usuário em português.** Código e identificadores podem ficar em inglês.
- **Nunca edite `MediaServiceCore/` nem `SharedModules/`.** São submódulos do upstream (yuliskov) e precisam continuar atualizáveis. Qualquer adaptação vai para o nosso código.
- **Siga as etapas do `PLANO.md` em ordem.** A etapa 1 (protótipo do motor) precisa funcionar antes de investir em telas.
- **Um APK só.** Nada de flavors separados para TV e celular. A interface é escolhida em tempo de execução.
- **Tire do app, em vez de esconder.** Se o usuário não pediu uma tela ou um botão, não construa.
