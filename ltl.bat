del /F /Q d:\ltl.log
cd d:\prog\ltl\bin
java -Dbrief -Dautoextra -cp .;..\lib\* LtlCli %1
del /F /Q %1
cd d:\