package br.com.uol.ps.amqp.support.log;

import org.apache.commons.logging.Log;

/**
 * Created by rafaelfirmino on 03/03/16.
 */
public final class FluentLogger {

    private final Log logger;
    private StringBuilder stringBuilder = new StringBuilder();

    public FluentLogger(final Log logger){
        this.logger = logger;
    }

    public Key key(final String key){
        return new Key(key, stringBuilder);
    }

    public class Key{
        private StringBuilder stringBuilder;

        public Key(String key, StringBuilder stringBuilder) {
            this.stringBuilder = stringBuilder;
            this.stringBuilder.append(key);
            this.stringBuilder.append("=");
        }

        public Value value(final Object value){
            return new Value(value, stringBuilder);
        }
    }

    public class Value{

        private StringBuilder stringBuilder;

        public Value(Object value, StringBuilder stringBuilder) {
            this.stringBuilder = stringBuilder;
            this.stringBuilder.append(value);
            this.stringBuilder.append(" ");
        }

        public Key key(final String key){
            return new Key(key, stringBuilder);
        }

        public String logError(){
            final String message = stringBuilder.toString().trim();
            logger.error(message);
            return message;
        }

        public String logInfo() {
            final String message = stringBuilder.toString().trim();
            logger.info(message);
            return message;
        }

        public String logWarn() {
            final String message = stringBuilder.toString().trim();
            logger.warn(message);
            return message;
        }
    }
}
