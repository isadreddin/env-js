var __context__ = __this__;

var Envjs = Envjs || 
	require('envjs/platform/core').Envjs;
	require('local_settings');

Envjs.platform       = "V8 RubyRacer";
Envjs.revision       = Ruby.CONFIG.ruby_version;

Ruby.ARGV.shift();
Envjs.argv = Ruby.ARGV;

Envjs.exit = function(){ Ruby.Process['exit!'](); };
