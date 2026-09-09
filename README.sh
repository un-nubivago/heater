#! /usr/bin/env sh

cat README.md \
    | sed '
        s_img/Heater\_Showcase\_1\.png_https://cdn.modrinth.com/data/g3jBz897/images/c1b280dacb9e25d5c0b29bbb9f9d0e096d8e30b6.png_;
        s_img/Heater\_Screen\.png_https://cdn.modrinth.com/data/g3jBz897/images/b68b1ac2d2e3253c2c3dd3f790662f7d9fe96edb.png_;
        s_img/Heater\_Recipe\.png_https://cdn.modrinth.com/data/g3jBz897/images/274da1bd3a20ff4bbbb441f71f9c8ce74a47a1d9.png_;
        s_img/Heat\_Pipe\_Recipe\.png_https://cdn.modrinth.com/data/g3jBz897/images/a86e1322ec5c0273ea234dd842cb5ec8eb97ecf2.png_;
        s_img/Thermostat\_Recipe\.png_https://cdn.modrinth.com/data/g3jBz897/images/af7fbab17256c8b32aec0d1b3d64cd9fb0749425.png_;
        s_https://github.com/0Starocean0_https://modrinth.com/user/StarOcean_;
        s_https://github.com/NivOridocs/burning_https://modrinth.com/mod/burning_;'
