twitter = Runtime.createAndStart("twitter","Twitter")
# replace security information with your own keys : 
#register your application at https://dev.twitter.com/ and obtain your own keys
twitter.setSecurity("yourConsumerKey","yourConsumerSecret", "yourAccessToken", "yourAccessTokenSecret")
twitter.configure()
#opencv must be a running service, with a camera capturing!!!!
twitter.uploadImage(opencv.getDisplay() , "text to upload");