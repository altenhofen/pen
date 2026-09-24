# pen - teclado de escrita à mão para Android

**Idioma:** [English](README.md) · Português

**pen** é um teclado Android em que você escreve com uma caneta stylus em vez de tocar teclas pequenas. Serve para quem lê e escreve normalmente, mas sofre com teclados comuns por causa de tremor, traços instáveis, levantadas acidentais da caneta, ou letras que os reconhecimentos genéricos confundem o tempo todo.

O produto é o teclado **pen ink**. Abra o app **pen** para calibrar suas letras, treinar gestos, adicionar palavras pessoais e guardar esse perfil.

O teclado aprende como *você* escreve. Treine letras em **Calibrar**. Quando um palpite vem errado e você apaga, a forma guardada se afasta daquela tinta. Quando você segue digitando, o acerto é reforçado. Com o tempo o teclado deve acompanhar a sua mão melhor do que um modelo único para todo mundo.

## Em que difere da digitação por escrita do Google

O caminho usual de escrita à mão no Android é a **escrita do Gboard** (o app separado Google Handwriting Input saiu de linha em favor do Gboard). O Google descreve esse layout como uma área em branco onde você **escreve palavras**. O reconhecimento é uma rede neural treinada com a escrita de muita gente. Os pontos do toque viram curvas de Bézier, uma rede recorrente propõe letras, e um **modelo de idioma** favorece sequências comuns naquela língua (por exemplo “sch” em alemão). Os modelos rodam no aparelho. O Gboard oferece escrita em muitos idiomas. Dedo e stylus escrevem. Na maior parte dos idiomas ele também tenta inserir espaços.

Essa descrição vem da [ajuda de escrita do Gboard](https://support.google.com/gboard/answer/9108773) e do [artigo de pesquisa de 2019 sobre escrita no Gboard](https://research.google/blog/rnn-based-handwriting-recognition-in-gboard/).

**pen** resolve outro problema. Um modelo genérico é forte em letra “típica” e em palavras inteiras. É fraco quando o seu `9` parece um `8`, quando um tremor parte uma letra em dois traços, ou quando você precisa do mesmo puxão inventado toda vez para apagar uma palavra.

| | Escrita do Gboard | pen |
|---|-------------------|-----|
| Objetivo | Transformar escrita típica em texto em muitos idiomas | Acompanhar *o seu* gesto, inclusive traços irregulares |
| Unidade | Palavras numa área em branco, com modelo de idioma | Letras, palavras ou um gesto treinado, depois de uma pausa de estabilização |
| Personalização | Um modelo compartilhado por sistema de escrita. O Google documenta personalização no aparelho para *digitação e voz* do Gboard, não um banco de letras que você treina | **Calibrar** guarda *os seus* desenhos. Manter ou apagar enquanto escreve também desloca essas formas. **Minhas palavras** cobre nomes que o modelo genérico não conhece |
| Controle motor | Velocidade da escrita e espessura do traço nas configurações do Gboard | A **janela de estabilização** espera levantadas acidentais. A tela de tinta é só stylus por padrão, para ignorar a palma apoiada |
| Offline | Modelos do Gboard no aparelho | Baixa uma vez um modelo **ML Kit Digital Ink** da Google e depois roda no aparelho. Se o modelo faltar, o teclado ainda usa a calibração |
| Backup | Conta / dados de digitação aprendidos no Gboard | **Exportar perfil** / **Importar perfil** criptografados com *as suas* formas, palavras, gestos e ajustes motores |
| Gestos | Ações de rabisco prontas em alguns modos stylus de tablet | Você desenha a forma. Em **Calibrar**, a aba **Gestures** liga essa forma a uma ação (apagar palavra, copiar, esconder o teclado e outras) |

pen ainda usa tecnologia da Google para palpites de palavra. O [ML Kit Digital Ink](https://developers.google.com/ml-kit/vision/digital-ink) é o reconhecedor de primeira passagem quando o modelo está baixado. A diferença é a camada em volta. Letras calibradas, memória de palavras e o dicionário pessoal podem mudar a ordem. Quando o ML Kit não responde, a calibração ainda pode inserir uma letra.

## Recursos

### Teclado pen ink

Ative **pen ink** na lista de teclados na tela do Android e toque num campo de texto.

- **Área de tinta.** Escreva com uma stylus ativa. Dedo e caneta passiva ficam desligados até você ligar **Dedo e caneta passiva** nas configurações. As teclas de baixo ainda aceitam o dedo.
- **Pausa de estabilização.** Depois que você levanta a caneta, a área espera (padrão 600 ms, faixa 300–1200 ms) para um tremor não cortar a letra no meio. Em seguida a tinta some e o melhor palpite é inserido.
- **Faixa de sugestões.** Até cinco alternativas. O palpite já inserido aparece em negrito. Toque outro chip para trocar a última inserção, enquanto ela ainda for o texto antes do cursor.
- **espaço**, **⌫**, **↵.** Espaço insere um espaço. Backspace apaga e, se você fizer isso logo após um palpite, trata o palpite como errado. Enter dispara a ação do campo (ou uma quebra de linha).
- **Estado do modelo.** Enquanto o modelo baixa, aparece **Baixando modelo de escrita**. Se o modelo não puder rodar, aparece **Offline, usando a calibração**.
- **Campos privados.** Campos de senha, e apps que recusam aprendizado personalizado, ainda recebem texto. Não ensinam o reconhecedor.

### Configurações da caneta

Abra o app **pen** (ou a entrada de configurações do teclado). O título da tela é **Configurações da caneta**.

- **Espaço após palavra inteira.** Depois de um palpite com mais de um caractere, insere um espaço. Um sinal de pontuação seguinte pega esse espaço e o coloca depois da marca. Desligado por padrão.
- **Reconhecer espaços na escrita.** Ligado, lacunas que o modelo de palavra vê viram espaços. Desligado, um grupo de tinta estabilizado conta como um só token (espaços do modelo são removidos). Desligado por padrão.
- **Janela de estabilização.** 300–1200 ms. Padrão 600 ms. Também vale nas telas de Calibrar e Minhas palavras.
- **Espessura do traço.** 2,0–16,0 dp. Padrão 6,0 dp.
- **Dedo e caneta passiva.** Permite toque na área de tinta *do teclado*. Desligado por padrão. Calibrar e o treino de Minhas palavras já aceitam dedo, stylus e mouse.
- **Definir como teclado padrão.** Abre a lista de métodos de entrada do Android para você ativar **pen ink**. O app não liga o interruptor sozinho.
- **Idioma.** **Idioma do app** pode ser Padrão do sistema, inglês, português ou espanhol. **Idioma da escrita** pode seguir o app ou ser inglês, português, espanhol, francês, alemão ou italiano para o modelo ML Kit.

### Calibrar

**Calibrar** ensina ao teclado as suas formas.

- **Glyphs.** Escolha qualquer conjunto de `0–9`, `A–Z` e `a–z`. Escreva cada caractere selecionado. Cada amostra vira um molde pessoal. As formas-semente de `0–9` e `a–z` continuam lá. As suas amostras são extras.
- **Gestures.** Associe um desenho seu a uma ação de texto. Cada ação precisa de pelo menos cinco amostras antes de ser salva como treinada. Se um desenho estabilizado casar com um gesto treinado de forma mais clara do que com uma letra, o teclado executa a ação em vez de inserir texto.

Os nomes **Glyphs** e **Gestures** aparecem em inglês nessa tela.

Ações de gesto que você pode treinar:

- Caixa da última palavra (minúsculas, maiúsculas, capitalizar, ciclar)
- Apagar última palavra, apagar linha, apagar tudo
- Desfazer último gesto
- Cursor no início/fim da linha e no início/fim do campo
- Selecionar última palavra, selecionar tudo
- Copiar, recortar, colar
- Inserir quebra de linha, inserir tab
- Trocar os dois últimos caracteres
- Esconder o teclado, trocar de teclado

### Minhas palavras

**Minhas palavras** é um dicionário pessoal para nomes e tokens que o modelo de palavra não conhece. O treino opcional (até cinco desenhos por palavra) ajuda o teclado a reconhecer essa palavra pela forma, não só pelo resgate de grafia contra os palpites do modelo.

### Exportar e importar perfil

**Exportar perfil** grava um arquivo criptografado `pen-profile.penbak`. Você escolhe uma senha de pelo menos oito caracteres. Essa senha não pode ser recuperada. Sem ela o arquivo não pode ser lido.

O arquivo inclui formas de letras calibradas, amostras de palavras, Minhas palavras, gestos treinados e os ajustes motores.

**Importar perfil** substitui o perfil no aparelho por esse arquivo. Exportações novas pedem a senha. Backups antigos sem criptografia ainda importam se você os escolher.

## Como funciona

1. Os traços são capturados na área de tinta. Tipos de ponteiro recusados (um dedo com o modo só-stylus ligado) nunca viram tinta.
2. Depois da janela de estabilização, o teclado procura um **gesto treinado**. Um gesto confiante que casa melhor do que uma letra dispara a ação ligada.
3. Senão, várias fontes respondem ao mesmo tempo.
   - **ML Kit Digital Ink** (modelo baixado, depois no aparelho) propõe textos, usando um trecho curto antes do cursor como contexto.
   - **Moldes de letra** comparam a tinta às formas guardadas (vizinho mais próximo numa trajetória normalizada). Pesam mais quando o modelo de palavra está mudo ou o melhor palpite tem um só caractere.
   - **Memória de palavras** lembra palavras inteiras que você já aceitou.
   - **Minhas palavras** pode reforçar grafias próximas das entradas do seu dicionário.
4. A maior pontuação combinada é **inserida na hora** no app em que você está escrevendo. A faixa serve para corrigir, não para confirmar.
5. A próxima ação ensina os moldes de letra (quando o aprendizado é permitido).
   - Seguir escrevendo, ou tocar espaço/enter → o último acerto de letra se aproxima daquela tinta.
   - Backspace (ou o cursor saltar para a esquerda) em cerca de três segundos → o último acerto de letra se afasta.
   - Tocar outra sugestão troca o texto. Tinta em forma de palavra pode ser lembrada da próxima vez.

Nada desse banco pessoal é enviado por este app. O único passo de rede da Google é baixar o modelo de escrita ML Kit do idioma escolhido.

Notas de planejamento e marcos mais antigos estão em [docs/PRD.md](docs/PRD.md). Esse documento descreve a intenção do produto. Este README descreve o que o app faz agora. Alguns itens do PRD (controle ao vivo de ambiguidade, tela de tinta em tela cheia no IME, reconhecedor “contextual bandit”) não são o teclado enviado hoje.

## O que você precisa

| Requisito | Detalhes |
|-----------|----------|
| Aparelho | Telefone ou tablet Android (Android 8.0 / API 26 ou mais novo) |
| Entrada | Stylus ativa recomendada. O dedo funciona no teclado se você ligar **Dedo e caneta passiva** |
| Compilação (opcional) | [Android Studio](https://developer.android.com/studio) se você instala a partir do código |

## Instalar a partir do código (desenvolvedores e testers)

1. **Obtenha o código**
   ```bash
   git clone https://github.com/altenhofen/pen.git
   cd pen
   ```

2. **Abra no Android Studio**
   Abra a pasta `pen`. Espere o Gradle sincronizar (a primeira vez pode levar vários minutos).

3. **Conecte um aparelho ou inicie um emulador**
   Ative **Opções do desenvolvedor** e **Depuração USB** num aparelho físico, ou crie um emulador no Device Manager.

4. **Execute o app**
   Clique em **Run** (play verde) ou, no terminal:
   ```bash
   ./gradlew :app:installDebug
   ```

5. **Rode os testes** (opcional)
   ```bash
   ./gradlew test
   ```

## Ligar o teclado pen ink (todo mundo)

Depois que o app estiver instalado:

1. Abra **Configurações → Sistema → Idiomas e entrada** (os nomes mudam um pouco conforme o fabricante), ou toque em **Definir como teclado padrão** no app pen.
2. Toque em **Teclado na tela** → **Gerenciar teclados na tela**.
3. Ative **pen ink**.
4. Abra qualquer app com um campo de texto, toque no campo e depois no ícone de teclado na barra de navegação ou de status.
5. Escolha **pen ink**.

Você deve ver uma área de desenho, uma faixa de sugestões e **espaço** / backspace / enter. Escreva um caractere ou uma palavra com a stylus. Depois de levantar a caneta e pausar, o melhor palpite é inserido. Se o texto vier errado, toque noutro chip ou use **Backspace** em seguida.

Para letras que o modelo genérico confunde, abra **Calibrar** e escreva você mesmo esses caracteres.

## Organização do projeto

```
app/          Aplicativo Android, tela de configurações e IME (serviço de teclado)
docs/         Requisitos de produto e planejamento
gradle/       Configuração de build (projeto Android padrão)
```

Áreas principais de código:

- `PenInputMethodService` — o serviço de teclado
- `DrawingCanvasView` — área de tinta e tempo de estabilização
- `InkModel` — encapsula o ML Kit Digital Ink
- `recognition/` — casamento de letras e gestos, memória de palavras, mistura de sugestões, armazenamento no aparelho
- `calibration/` — sessões de Calibrar para glifos e gestos
- `profile/` — exportação e importação criptografadas

## Contribuir

Contribuições são bem-vindas. Leia [CONTRIBUTING.md](CONTRIBUTING.md) antes de abrir um pull request.

## Segurança

Para relatar um problema de segurança, veja [SECURITY.md](SECURITY.md). Não abra issues públicas para relatos sensíveis.

## Licença

Este projeto está sob a [licença MIT](LICENSE).
