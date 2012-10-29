//-----------------------------------------------------------------
//
// Library for clean interactions with Monome devices via 
// serialosc
//
//  __ Tristan Strange
//     <tristan.strange@gmail.com>
//
//  __ Forked from serialio version by Daniel Jones availiable at
//     <https://github.com/ideoforms/sc-monome/>
// 
// Run serialosc with:
//    ./serialosc
//
// Note the port number your monome is running on
//
//    m = Monome.new(port: <port number>);
//    m = Monome.new;
//    m.action = { |x, y, on|
//      [x, y, on].postln;
//    };
//    m.led(5, 6, 1);
//    m.ledRow(4, 255);
//    m.clear;
//
//-----------------------------------------------------------------

Monome
{
	var <host, <port;
	var <prefix;
	var <height, <width;
	var <>target;
	var <>action;

	var <pressed;

	*new { |host = "127.0.0.1", port = 8080, prefix = "/monome", height = 8, width = 8|
		^super.newCopyArgs(host, port, prefix, height, width).init;
	}
	
	*emu { |port = 57120, width = 8, height = 8|
		// spawn emulator
		MonomEm.new(port: port, width: width, height: height);
		^this.new(port: port, width: width, height: height).init;
	}
	
	init {
		pressed = [];
		height.do({ pressed = pressed.add(Array.fill(width, 0)) });		
		
		OSCFunc({ |msg|
			pressed[msg[2]][msg[1]] = msg[3];
			if (action.notNil)
			   { action.value(msg[1], msg[2], msg[3]); };
		}, prefix ++ "/grid/key");

		target = NetAddr(host, port);

		this.port_(NetAddr.langPort)
	}

	prefix_ { |pre|
		prefix = pre;
		target.sendMsg("/sys/prefix", prefix);
	}

	port_ { |p| 
		port = p;
		target.sendMsg("/sys/port", port);
	}

	host_ { |h| 
		host = h;
		target.sendMsg("/sys/host", host);
	}

	rotation_ { |rotation| 
		target.sendMsg("/sys/rotation", rotation);
	}

	led { |x, y, on = 1|
		target.sendMsg(prefix ++ "/grid/led/set", x.asInteger, y.asInteger, on.asInteger);
	}

	ledRow { |xOffset, y, on = 255|
		target.sendMsg(prefix ++ "/grid/led/row", xOffset.asInteger, y.asInteger, on.asInteger);
	}

	ledCol { |x, yOffset, on = 255|
		target.sendMsg(prefix ++ "/grid/led/col", x.asInteger, yOffset.asInteger, on.asInteger);
	}
	
	intensity { |i|
		target.sendMsg(prefix ++ "/grid/led/intensity", i);
	}

	clear { |on|
		this.all(on);
	}

	all { |on|
		on = on ?? 0;
		target.sendMsg(prefix ++ "/grid/led/all", on);
	}
}

