## Start Rserve and load library

# Install relevant packages as administrator (R -> right click -> Als Administrator ausf�hren)
install.packages("rJava") # system.file("jri",package="rJava")
install.packages('Rserve')
install.packages('plm')
install.packages('lpSolve')
install.packages('rms') # dauert ewig! ohne Kompilieren ausw�hlen

R.home()
.libPaths()

library(rJava)
library(Rserve)
library(plm)
library(lpSolve)
library(rms)
Rserve(args="--no-save")


