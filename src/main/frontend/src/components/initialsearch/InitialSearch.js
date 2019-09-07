import React, { useRef, useContext, useState } from 'react';
import { SearchContext } from '../searchcontext/SearchContext';
import { Button } from '@material-ui/core';

import { BASE_URL } from '../../App';
import './InitialSearch.css';
import { withRouter, Link } from 'react-router-dom';
import InputWithTitleAndSubmit from '../common/InputWithTitleAndSubmit';


function InitialSearch(props) {
    // const {searchCtx} = useContext(SearchContext);
    const [state, setState] = useContext(SearchContext);
    let query = "";
    let inputRef = useRef();
    let [errorMessage, setErrorMessage] = useState()
    let [error, setError] = useState(false)

    function submitQuery() {
        if (!query) return;
        setError(false)
        setErrorMessage(null)
        state.searchQuery.searchStr = query;
        // console.log("state: " + JSON.stringify(state));
        let resStatus = 0

        fetch(`${BASE_URL}/search`,
            {
                method: "POST",
                headers: {
                    Accept: 'application/json',
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(state.searchQuery)
            })
            .then(function (res) {
                resStatus = res.status
                return res.json();
            })
            .then(function (data) {
                switch (resStatus) {
                    case 200:
                        if (data.success) {
                            props.history.push({ pathname: '/result', state: { data: data.data } });
                        } else {
                            setError(true)
                            setErrorMessage(data.data.searchStr)
                        }
                        break
                    case 400:
                        setError(true)
                        setErrorMessage(data.data.searchStr)
                        break
                    default:
                        console.log('unhandled')
                        break
                }
            })
    }

    const handleKeyDown = (e) => {
        if (e.key === 'Enter') {
            submitQuery();
        }
    }
    const updateValue = (e) => {
        let val = e.target.value
        query = val
    }


    return (
        <div>
            <Link to="/" >
                <Button variant="text" color="primary">
                    Back to Crawl
            </Button>
            </Link>

            <div className="center-div">
                <InputWithTitleAndSubmit
                    title="Search"
                    label="Type query here to search in crawled results"
                    disabled={false}
                    setValue={updateValue}
                    submitInput={submitQuery}
                    inputReference={inputRef}
                    titleRef={null}
                    handleKeyDown={handleKeyDown}
                    errorMessage={errorMessage}
                    error={error}
                />

            </div>
        </div>
    );
}

export default withRouter(InitialSearch);