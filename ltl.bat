cd d:\prog\ltl\bin
rem java -Dbrief -Dautoextra -cp .;..\lib\* LtlCli %1 %2
java -Dbrief -Dautoextra -cp .;..\lib\* LtlCli %1 7
del /F /Q %1
cd d:\