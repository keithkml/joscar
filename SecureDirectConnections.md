This page is part of the AimSecureIm documentation.

## Encrypted direct connection, file transfer, Get File ##

Encrypted direct IM, file transfer, and Get File is simply performed over an SSL connection in place of a normal TCP connection. The SSL server (whomever is being connected to) normally (and should) requires "client auth"; that is, the user who is acting as the SSL server should request the certificate of the SSL client (the other user).

Due to what must be a bug in AOL's implementation, when an AIM 5.2 client is the SSL server, he sends a list of "trusted certificate authorities" that contains only ''his'' identity. Normally this would mean that he would only accept client certificates which were signed by his own certificate; in AIM's case, however, it means nothing, and must be ignored. Unfortunately, in Java, it takes some work to ignore it. When connecting as a client, you must define your own `KeyManager` like so:

```
// we assume these are defined
SSLContext context;
KeyManagerFactory kmf;
TrustManager[] tms;
SecureRandom random;

// find the first X.509 key manager - JSSE only uses the first manager
// of each type in the array, so any X.509 key managers after the
// first are ignored anyway
KeyManager[] kms = kmf.getKeyManagers();
int index = -1;
for (int i = 0; i < kms.length; i++) {
    if (kms[i] instanceof X509KeyManager) {
        index = i;
        break;
    }
}

// we assume this worked
assert index != -1;

// replace the X.509 key manager in the array with our own
final X509KeyManager xkm1 = (X509KeyManager) kms[index];

kms[index] = new X509KeyManager() {
    public PrivateKey getPrivateKey(String string) {
        return xkm1.getPrivateKey(string);
    }
    public X509Certificate[] getCertificateChain(String string) {
        return xkm1.getCertificateChain(string);
    }
    public String[] getClientAliases(String string, Principal[] principals) {
        return xkm1.getClientAliases(string, principals);
    }
    public String[] getServerAliases(String string, Principal[] principals) {
        return xkm1.getServerAliases(string, principals);
    }
    public String chooseServerAlias(String string, Principal[] principals,
            Socket socket) {
        return xkm1.chooseServerAlias(string, principals, socket);
    }
    public String chooseClientAlias(String[] strings, Principal[] principals,
            Socket socket) {
        // ignore principals of trusted CA's
        return xkm1.chooseClientAlias(strings, null, socket);
    }
};

context.init(kms, tms, random);
```

Notice how the list of trusted certificate authorities (principals) is ignored, and `null` is passed to the real `chooseClientAlias`.

To require client authorization when acting as the SSL server in Java, one can use code like this:

```
// we assume this is defined
SSLServerSocket sss;

sss.setNeedClientAuth(true);
```

You will most likely want to define custom `TrustManager`s in both cases to ensure that the certificates received from the other party match those provided by the AIM server.

### Secure connections over the AOL Proxy Server ###

Secure connections over the AolProxyServer are special because the connection to the proxy server itself is not an SSL connection. Rather, it is a normal TCP connection, and the AOL proxy server commands are sent plain, not encrypted at all. When the proxied connection begins, however, the connection should behave just like an SSL connection would had it just connected (or been connected to). The AOL proxy server connection needs to become a "new" SSL connection once the proxy initialization has finished.

To do this in Java, you can "wrap" the socket in an `SSLSocket` using the `createSocket` method of `SSLSocketFactory`. (Any `SSLSocket` can be transformed into an SSL server socket with some configuration (see the [setUseClientMode method in SSLSocket](http://java.sun.com/j2se/1.4.2/docs/api/javax/net/ssl/SSLSocket.html#setUseClientMode(boolean))).) This can be done after the proxy initialization sequence is complete.