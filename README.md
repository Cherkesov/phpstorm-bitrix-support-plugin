### Как настроить проект

1) Открыть в IDEA

2) Выбрать корень, F4

3) Добавить SDK типа IntelliJ Platform, в качестве пути к SDK указать директорию установки PhpStorm

4) Для проекта пометить этот SDK как активный

4) Создать бибиотеку, добавит в нее файлы шторма

- PHP_STORM_INSTALL_DIR\plugins\php\lib\php.jar
- PHP_STORM_INSTALL_DIR\plugins\php\lib\php-openapi.jar<br/><br/>
    
    Библиотеку добавить к модулю (проекту)
    
    В настройках модуля для библиотеки ставим scope "Provided"
    
5) Создаем артефакт JAR > From module with dependencies

6) Сохраняем настройки проекта

7) Main menu > Build > Build artifact