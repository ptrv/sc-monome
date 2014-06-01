//-----------------------------------------------------------------
//
// Visual emulator for the Monome 40h.
// Sends OSC messages to the specified host/port in the same
// format as the 40h, and responds to LED actions.
//  __ Daniel Jones
//     http://www.erase.net
//
// Usage:
//    e = MonomEm.new(host, port);
//    m = Monome.new(host, port);
//    m.action = { |x, y, on| [x, y, on].postln };
//    m.led(5, 6, 1);
//
// or:
//    m = Monome.emu;
//    m.led(6, 7, 1);
//
//-----------------------------------------------------------------


MonomEm
{
    var <host, <port;
    var prefix;
    var <width, <height;
    var deviceName;
    var qwerty;
    var window, matrix;
    var buttonsize = 16;
    var keymap;
    var addr, target;
    var resps;

    *new { |host = "127.0.0.1", port = 8080, prefix = "/monome", width = 8, height = 8, deviceName = "m64", querty=false|
        ^super.newCopyArgs(host, port, prefix, width, height, deviceName, querty).init;
    }

    init {
        window = Window.new(deviceName, Rect(30, 50, buttonsize * (width * 2 + 1), buttonsize * (height * 2 + 1)), false, true);
        height.do({ |y|
            matrix = matrix.add([]);
            width.do({ |x|
                var button;
                button = Button(window, Rect(buttonsize + (x * buttonsize * 2), buttonsize + (y * buttonsize * 2), buttonsize, buttonsize));
                button.states = [
                    [ "", Color.gray, Color.gray ],
                    [ "", Color.green, Color.green ]
                ];

                button.mouseDownAction = { |button|
                    this.press(x, y, 1);
                };
                button.mouseUpAction = { |button|
                    this.press(x, y, 0);
                };

                // button.action = { button.value = 1 - button.value };
                matrix[y] = matrix[y].add(button);
            });
        });

        if(qwerty,
            {
                keymap = [
                    [ $1, $2, $3, $4, $5, $6, $7, $8 ],
                    [ $q, $w, $e, $r, $t, $y, $u, $i ],
                    [ $a, $s, $d, $f, $g, $h, $j, $k ],
                    [ $z, $x, $c, $v, $b, $n, $m, $, ],
                    [ $!, $@ ,$#, $$, $%, $^, $&, $* ],
                    [ $Q, $W, $E, $R, $T, $Y, $U, $I ],
                    [ $A, $S, $D, $F, $G, $H, $J, $K ],
                    [ $Z, $X, $C, $V, $B, $N, $M, $< ],
                ];
            },{
                keymap = [
                    [ $1, $2, $3, $4, $5, $6, $7, $8 ],
                    [ $q, $w, $e, $r, $t, $z, $u, $i ],
                    [ $a, $s, $d, $f, $g, $h, $j, $k ],
                    [ $y, $x, $c, $v, $b, $n, $m, $, ],
                    [ $!, $" ,$§, $$, $%, $&, $/, $( ],
                    [ $Q, $W, $E, $R, $T, $Z, $U, $I ],
                    [ $A, $S, $D, $F, $G, $H, $J, $K ],
                    [ $Y, $X, $C, $V, $B, $N, $M, $; ],
                ];
        });


        window.view.keyDownAction = { |view, char, modifiers, unicode, keycode|
            keymap.do({ |row, y|
                if (row.indexOf(char).notNil)
                { this.press(row.indexOf(char), y, 1) };
            });
        };
        window.view.keyUpAction = { |view, char, modifiers, unicode, keycode|
            keymap.do({ |row, y|
                if (row.indexOf(char).notNil)
                { this.press(row.indexOf(char), y, 0)};
            });
        };
        window.onClose = {
            resps.do({ |resp|
                resp.free;
            });
        };

        CmdPeriod.add({ window.close; });

        window.front;

        addr = NetAddr(host, port);
        resps = [];
        resps = resps.add(OSCFunc({ |msg| msg.postln; this.led(msg[1], msg[2], msg[3]) },prefix ++ "/grid/led/set", addr));
        resps = resps.add(OSCFunc({ |msg| msg.postln; this.ledRow(msg[1], msg[2], msg[3], msg[4]) },prefix ++ "/grid/led/row", addr));
        resps = resps.add(OSCFunc({ |msg| msg.postln; this.ledCol(msg[1], msg[2], msg[3]) },prefix ++ "/grid/led/col", addr));

        target = NetAddr("127.0.0.1", port);
    }

    close {
        window.close;
    }

    press { |x, y, on|
        target.sendMsg(prefix ++ "/grid/key", x, y, on);
    }

    led { |x, y, on|
        postln("led " ++ [ x, y ] ++ ": " ++ on);
        defer({ matrix[y][x].value = on; matrix[y][x].doAction; });
    }

    ledRow { |xOffset, y, on = 255, on2|
        if(on2.notNil,
            {on = on + (on2 * 256)});
        defer({
            width.do({ |x|
                var pow = (2 ** x.mod(8)).asInteger;
                matrix[y][x].value = ((pow & on) > 0).binaryValue;
                matrix[y][x].doAction;
            });
        });
    }

    ledCol { |x, yOffset, on = 255|
        defer({
            height.do({ |y|
                var pow = (2 ** y).asInteger;
                matrix[y][x].value = ((pow & on) > 0).binaryValue;
                matrix[y][x].doAction;
            });
        });
    }
}

