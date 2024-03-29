TODO's

MULTICAST
- Verificar se os valores do IP e porto são apropriados (de momento estão iguais à ficha 3)
- Determinar como dar Downloader/Barrel IDs (e se é necessário)

DOWNLOADER
- ignorar pontuação
- tratar de por tudo minúsculo

SEARCH
- relevância
- outras páginas

PROJECT MANAGER
- ver dos portos -> ligação do gateway para os barrels (escolher gateway)
- ver de threads

MARIANA
- ordenar por relevância
- acabar status

SAULO
- search -> interseção
- agrupados 10 em 10

PERGUNTAS
- threads nos barrels
- balanceamento da carga nas pesquisas sobre o storage barrels
- is hashmap o/r mapping? melhor forma de guardar info nos barrels


ERROR HANDLING : DOWNLOADER
- java.net.ConnectException: (quando se inicia o downloader sem iniciar a queue)
- java.net.SocketTimeoutException: Read timed out (quando fica à espera de receber URLS durante demasiado tempo e não recebe mais) -> 
já aconteceu receber esse erro e imediatamente a seguir receber mais links a seguir
- javax.net.ssl.SSLHandshakeException: PKIX path building failed: sun.security.provider.certpath.SunCertPathBuilderException: unable to find valid certification path to requested target
(literalmente no fucking clue what this is, não mandou o downloader abaixo tho)
- Exception in thread "main" java.lang.IllegalArgumentException: The supplied URL, 'javascript:window.__tcfapi('displayConsentUi', 2, function() {} );', is malformed. 
Make sure it is an absolute URL, and starts with 'http://' or 'https://'. See https://jsoup.org/cookbook/extracting-data/working-with-urls
  (isto não deveria ser handled pelo MalformedURLwhatever?) (foi o único destes três que efetivamente fez o Downloader parar)
- java.lang.NullPointerException -> quando não existe firstParagraph
- org.jsoup.HttpStatusException: HTTP error fetching URL. Status=403, URL=[https://help.nytimes.com/hc/en-us/articles/115015727108-Accessibility] no downloader


EXTRA:
- organizar melhor Barrel, classe para Hashmaps
- Loop infinito de ler URLQueue -> descobrir como fazer o loop parar quando o programa acaba 
- verificar citações "ative as notificações do sapo"




