# Gothic HUD (Fabric, Minecraft 26.2)

Клиентский мод: рамка вокруг инвентаря, игрок по торс, свечи и капающая кровь, паутина по краям экрана,
пиксельная кровь при ударе, сердцебиение и дёргающаяся виньетка.

## Как получить готовый jar без установки Java (через GitHub)
1. Зарегистрируйся на github.com (бесплатно) и нажми New repository, назови `gothic_hud`, создай.
2. В репозитории: Add file → Upload files. Перетащи ВСЁ содержимое этой папки (src, build.gradle, settings.gradle,
   gradle.properties и папку .github) и нажми Commit changes.
   Если папка .github не загрузилась: Add file → Create new file, в имя вставь `.github/workflows/build.yml`
   и вставь туда текст из файла `github-workflow-build.yml`.
3. Открой вкладку Actions. Сборка стартует сама, занимает 3-5 минут.
4. Когда появится зелёная галочка, открой запуск и внизу в Artifacts скачай `gothic_hud-jar`.
5. Распакуй, jar положи в папку `mods` вместе с Fabric API для 26.2.

Если сборка красная: открой запуск, скопируй текст ошибки и пришли мне, я поправлю код.

## Ручная сборка
JDK 25 и команда `gradle build`, jar окажется в `build/libs/`.
