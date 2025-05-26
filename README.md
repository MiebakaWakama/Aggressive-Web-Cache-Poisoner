# Overview

**Note**: if you are new to web cache poisoning and deception, I recommend checking out James Kettle research here:  [Web Cache Entanglement: Novel Pathways to Poisoning](https://portswigger.net/research/web-cache-entanglement)
Static files such as images, JavaScripts, text files are typically accessible anonymously,  do not contain sensitive information, and such, are considered safe for public caching in CDNs such as Akamai, CloudFront, CloudFlare, etc. Content Delivery Networks (CDNs) or reverse-proxies may intentionally or accident be configured for 
Aggressive caching of these static files. Aggressive caching in the sense that regardless of the HTTP response returned e.g 403, 404, 400, 503, etc, by the application, provided the URL of the resource requested points to a static resource, the response will be cached regardless, poisoning the original resource or HTTP response and leading to a Denial-of-Service (DoS) of the static files. 
Following my review the work of James Kettle on web cache poisoning, HTTP headers are mostly not keyed during the caching of resources by CDNs or reverse-proxies. Hence, any invalid HTTP header that may draw an error response from the application such as a simple header with a wrong value e.g Content-Length: xyz, should suffice to poison static resources that are aggressively cached. 
In my experience, these poisoning  last for a few seconds of 5 -10 secs. However, an attacker may persist such poisoning by automating repeatedly sending the poisoning request, making it last longer. 


# Exploitation
I have found out that during my penetration testing engagements that while some static resources may not be aggressively cached, this may not be the case for others. Moreover, a particular static file type or extension may not be aggressively cached in one URL path or location but may be in a different location. Hence, exploitation demands identifying those resources, extensions or paths that are aggressively cached, if available. In my experience, doing this manually can be very cumbersome and may lead to frustrations, especially since it requires playing with cache-busters and the determination of a successful exploit requires sending the poisoning request multiple times, which is why I wrote this BurpSuite extension.  

## What does this mean for web applications?

At first thought, poisoning or DoS'ing static files may not immediately appear impactful. "You took out an image or JavaScript file, so what?" Well, for one,  DoS'ing all the images in may lead to complete defacement of an application or adversely affect users' experience on the application. DoS'ing JavaScript files may render an application completely nonfunctional, particularly, in those applications that rely on JavaScript for rendering and navigation. The issue is quickly exacerbated for SPA web applications where DoS'ing of an JavaScript file may lead to complete DoS'ing of the entire application.

## How to Use

 1. Identify any HTTP header + value combination that may elicit an error response, such as "Content-Length: xyz" for a static resource
 2. Once Identified, copy the HTTP header + value combination and input it in the input text field of the extension
 3. Right click on the request made to the static resource, and from the Extensions context menu, click "send to AggressiveWCP".  It is best to remove any "If-Modified" or "if-Not-Modified" headers first before sending to the extension.
 4. You should see 18 requests and responses in total being sent, The first 9 requests are the poisoning requests being sent, and the last 9 requests are the original requests being sent again. If the last 9 requests still resulted in the the error responses returned, i.e., contain same response for the poisoning requests, then the endpoint is vulnerable and the attack worked and vice versa.

##  Mitigation

 Disable the caching of HTTP error responses returned when static resources are accessed. 

## Note

1. Although this extension facilitates poisoning of static resources in web caches via HTTP headers, other DoS'ing techniques are applicable. 

3. Aggressive caching of static resources also opens the door or a pathway to web cache deception, particularly, in situations were appending extensions of a static resources e.g. (.jpg. .js, .woff) results in the same response returned when a URL path is requested, forcing the caching of the resource that may contain sensitive information.
