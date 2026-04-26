package net.ljcomputing.camelpoc.util;

// Copyright 2026 James G Willmore
// 
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
// 
//     https://www.apache.org/licenses/LICENSE-2.0
// 
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

// Source - https://stackoverflow.com/a/15580772
// Posted by Steven
// Retrieved 2026-04-25, License - CC BY-SA 3.0

public class Mapper
{
    public static void main(String[] args)
    {
        int zoom = 16;
        double lat = 40.6950153d;
        double lon = -74.2837219d;
        System.out.println("https://tile.openstreetmap.org/"
            + getTileNumber(lat, lon, zoom) + ".png");
    }

    public static String getTileNumber(double lat, double lon, int zoom)
    {
        int xtile = (int)Math.floor((lon + 180) / 360 * (1<<zoom));
        int ytile = (int)Math.floor((1 - Math.log(Math.tan(Math.toRadians(lat))
            + 1 / Math.cos(Math.toRadians(lat))) / Math.PI) / 2 * (1<<zoom));
        return ("" + zoom + "/" + xtile + "/" + ytile);
    }
}
