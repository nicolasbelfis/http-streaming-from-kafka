import React, {useState} from 'react';
import * as PropTypes from "prop-types";
import {TweetList} from "./components/TweetList";
import {CountTag} from "./CountTag";
import {CountTagState} from "./CountTagState";

TweetList.propTypes = {list: PropTypes.arrayOf(PropTypes.any)};

export default function App() {

    const [tweets, setTweets] = useState([]);
    const [connexion, setConnexion] = useState({});
    const [connected, setConnected] = useState(false);


    function startStreaming() {
        let sse = new EventSource('http://localhost:8080/stream/sseTweets', {withCredentials: false})
        sse.onerror = () => {
            // error log here
            // after logging, close the connection
            setConnected(false)
            sse.close();
        }
        sse.onmessage = e => getRealtimeData(JSON.parse(e.data));
        setConnexion(sse)
        setConnected(true)
    }

    function stopStreaming() {
        setConnected(false)
        connexion.close()
    }

    function computeNewState(prevState, data) {
        var newState = []
        prevState.forEach((value, index) => {
            if (prevState.length < 4) newState[index] = prevState[index]
            else if (index > 0 && value !== undefined && index < 4) newState[index - 1] = value
        })
        return [...newState, data]
    }

    function getRealtimeData(data) {

        setTweets(prevState => {
            return computeNewState(prevState, data);

        })
    }

    return (
        <div className="container">
            <div className="row border border-primary">
                <div className="col-sm-2">
                    <p>stream tweets real time</p>
                    <button className={connected ? "invisible" : "btn-primary"} onClick={() => startStreaming()}>start</button>
                    <button className={connected ? "btn-primary" : "invisible"} onClick={() => stopStreaming()}>stop</button>
                </div>
                <div className="col-sm-10 border-1">
                    <TweetList list={tweets}/>
                </div>
            </div>
            <div className="row border border-primary">
                <CountTag />
            </div>
            <div className="row border border-primary">
                <CountTagState />
            </div>
        </div>
    );
}
