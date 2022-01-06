package com.nikitavbv.disaster
package model

case class Disaster(id: String,
                    title: String,
                    location: Seq[DisasterLocation])

case class DisasterLocation(latitude: Double,
                            longitude: Double)
