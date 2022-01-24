# Forex-MTL
## Compile, Run, Test, Publish, Deploy:
1. The service is written using Scala 2.13, Http4s, cats, fs2 for handling REST requests.
2. To compile the code use
   ```sbtshell
   sbt compile   
   ```
3. To run the application locally:
   ```sbtshell
   sbt run
   ```
   
## Design
1. The Forex-MTL project helps in creating a proxy for OneFrame API service.
2. It uses caffiene cache to maintain the key values, since one frame api service is thirdparty.
3. Everything in the application is config driven, as a result it's very easy to deploy the application based on relevant values.
4. Custom Error handling is included, so that the necessary error responses in a descriptive manner is send back to the client.