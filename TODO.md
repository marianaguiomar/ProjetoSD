TODO's
MULTICAST
- Verificar se os valores do IP e porto são apropriados (de momento estão iguais à ficha 3)

DOWNLOADER
- tratar de por tudo minúsculo

SEARCH
- relevância
- outras páginas

PROJECT MANAGER
- ver dos portos -> ligação do gateway para os barrels (escolher gateway)
- ver de threads

MARIANA
- mudar status
- javadoc
- relatório RMI

SAULO
- implementar o confirmation para múltiplos barrels
- relatório multicast
- limpar no código do gateway

PERGUNTAS
- threads nos barrels
- balanceamento da carga nas pesquisas sobre o storage barrels
- como é que o gateway vai saber qual é o barrel que tem a informação que ele quer
- está correta a nossa implementação do multicast fiável?
- is hashmap o/r mapping? melhor forma de guardar info nos barrels


ERROR HANDLING : DOWNLOADER
- javax.net.ssl.SSLHandshakeException: PKIX path building failed: sun.security.provider.certpath.SunCertPathBuilderException: unable to find valid certification path to requested target
(literalmente no fucking clue what this is, não mandou o downloader abaixo tho)
- Exception in thread "main" java.lang.IllegalArgumentException: The supplied URL, 'javascript:window.__tcfapi('displayConsentUi', 2, function() {} );', is malformed. 
Make sure it is an absolute URL, and starts with 'http://' or 'https://'. See https://jsoup.org/cookbook/extracting-data/working-with-urls
  (isto não deveria ser handled pelo MalformedURLwhatever?) (foi o único destes três que efetivamente fez o Googol.Downloader parar)


EXTRA:
- Filtro bloom
- Indice particionado em duas partes



