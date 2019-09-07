import React, { useState, useRef } from 'react';
import { BASE_URL } from '../../App';
import './Crawl.css';
import usePolling from '../usepolling/usePolling'
import Button from '@material-ui/core/Button';
import { withRouter, Link } from 'react-router-dom';
import InputWithTitleAndSubmit from '../common/InputWithTitleAndSubmit';

function Crawl(props) {

    // const [url, setUrl] = useState(null);
    const [disabled, setDisbaled] = useState(false);
    let [errorMessage, setErrorMessage] = useState()
    let [error, setError] = useState(false)
    // const [magic, setMagic] = useState()
    let cnt = 0
    let url
    let titleReference = useRef();

    const [isPolling, startPolling, stopPolling] = usePolling({
        url: `${BASE_URL}/counter`,
        interval: 1000, // in milliseconds(ms)
        retryCount: 0, // this is optional
        onSuccess: (data) => {
            // console.log(data);
            if (data.success) {
                let newCnt = data.data.count
                if (cnt < newCnt) {
                    cnt = newCnt
                    data.data.count = cnt;
                } else if (cnt < 100) {
                    cnt++
                    data.data.count = cnt;
                } else {
                    data.data.count = cnt;
                    titleReference.current.innerText = "Finishing crawl job..."
                }
                return show_progress(data.data)
            } else {
                alert(data.data);
                return false;
            }
        },
        onFailure: (error) => {
            console.error(error);
            stopPolling()
        }, // this is optional
        method: 'GET',
        headers: {
            'X-Requested-With': 'XMLHttpRequest',
            'withCredentials': 'true'
        },
        isPolling: false,
    });

    let prog = useRef();

    function show_progress(counter) {
        // console.log("counter.count: " + counter.count)
        if (counter.finished) {
            // console.log("Crawl finished");
            stopPolling();
            props.history.push('/search');
            // navigate('/search');
            return false;
        }
        var progress1 = counter.count;
        // console.log("progress1: " + progress1)
        if (progress1 == null) {
            stopPolling();
            return false;
        }
        var progress2 = progress1 + 1;
        var progress3 = progress1 + 2;

        var magic = "linear-gradient(to right, lightskyblue " + progress1 + "% ,darkblue " + progress2 + "% , #FFFFFF " + progress3 + "%)";
        prog.current.style.background = magic;
        return true;
    };

    const handleKeyDown = (e) => {
        if (e.key === 'Enter') {
            submitCrawl();
        }
    }
    const updateValue = (e) => {
        let val = e.target.value
        url = val
    }


    function submitCrawl() {
        setError(false)
        setErrorMessage(null)

        setDisbaled(true);

        let resStatus = 0
        fetch(`${BASE_URL}/crawl`, {
            method: 'POST',
            headers: {
                Accept: 'application/json',
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({ "address": url, })
        }).then(response => {
            resStatus = response.status
            return response.json()
        })
            .then(res => {
                switch (resStatus) {
                    case 200:
                        console.log('success')
                        startPolling();
                        titleReference.current.style.textOverflow = "ellipsis"
                        titleReference.current.style.whiteSpace = "nowrap"
                        titleReference.current.innerText = "Crawling domain: " + res.data.domain + " ..."
                        break
                    case 400:
                        if (res.data.errorCode === 'DOMAIN_ALREADY_CRAWLED') {
                            console.log(res.data.errorMessage)
                            setDisbaled(false)
                            let errMsg = res.data.errorMessage
                            // `${res.data.errorMessage}|\n|You will be redirected to search`
                            setError(true)
                            setErrorMessage(errMsg)
                            // setTimeout(() => {
                            //     props.history.push('/search')
                            //   }, 3000)
                        } else {
                            setDisbaled(false)
                            console.log('Server error:\n' + JSON.stringify(res))
                            let errMsg = ""
                            Object.entries(res.data).map(([field, msg]) => {
                                errMsg = errMsg + msg
                                return errMsg
                            })
                            setError(true)
                            setErrorMessage(errMsg)
                        }
                        break
                    case 500:
                        console.log('server error, try again')
                        break
                    default:
                        console.log('unhandled')
                        break
                }
            })
            .catch(err => {
                console.error(err)
            })

    };

    return (
        <div>
            <Link to="/search">
                <Button variant="text" color="primary">
                    Go to Search
                </Button>
            </Link>

            <div className="center-div">
                <InputWithTitleAndSubmit
                    title="Crawl"
                    label="Type URL to Crawl here"
                    setValue={updateValue}
                    submitInput={submitCrawl}
                    disabled={disabled}
                    inputReference={prog}
                    titleRef={titleReference}
                    handleKeyDown={handleKeyDown}
                    errorMessage={errorMessage}
                    error={error}
                />

            </div>
            {errorMessage &&
                <div className="error">

                </div>

            }
        </div >

    );
}

export default withRouter(Crawl);