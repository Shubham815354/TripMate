package com.example.tripmate.myModel

data class Details(val title:String? , val thumbnail:Thumbnail,
                   val description:String?,val coordinates:Coordinates,val extract:String?)
data class Thumbnail(var source:String?)
data class Coordinates(val lat:String?, val lon:String?)
