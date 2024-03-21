TODO's

URL QUEUE
- Decidir a estrutura de dados -> ficou concurrent double deque, verificar se está correto
- Decidir onde deixar a queue (noutros ficheiros, num programa à parte)
- Ver a ordem de inserção

MULTICAST
- Verificar se os valores do IP e porto são apropriados (de momento estão iguais à ficha 3)
- Determinar como dar Downloader/Barrel IDs (e se é necessário)
- DOWNLOADER
  - alterar a parte toda de enviar
  - ver se é possível enviar objetos

DOWNLOADER
- Main: criar as palavras reservadas e removê-las
- Adicionar hiperlinks encontrados
- Loop infinito de ler URLQueue -> descobrir como fazer o loop parar quando o programa acaba


CLASSE URL
- Determinar onde a definir (depende de se podem enviar objetos por multicas ou não)

BARREL
- criar índice remissivo


NOTAS 
- antes de correr downloader, correr queue

ERROR HANDLING : DOWNLOADER
- java.net.ConnectException: (quando se inicia o downloader sem iniciar a queue)
- java.net.SocketTimeoutException: Read timed out (quando fica à espera de receber URLS durante demasiado tempo e não recebe mais) -> 
já aconteceu receber esse erro e imediatamente a seguir receber mais links a seguir
- javax.net.ssl.SSLHandshakeException: PKIX path building failed: sun.security.provider.certpath.SunCertPathBuilderException: unable to find valid certification path to requested target
(literalmente no fucking clue what this is, não mandou o downloader abaixo tho)
- Exception in thread "main" java.lang.IllegalArgumentException: The supplied URL, 'javascript:window.__tcfapi('displayConsentUi', 2, function() {} );', is malformed. 
Make sure it is an absolute URL, and starts with 'http://' or 'https://'. See https://jsoup.org/cookbook/extracting-data/working-with-urls
  (isto não deveria ser handled pelo MalformedURLwhatever?) (foi o único destes três que efetivamente fez o Downloader parar)


